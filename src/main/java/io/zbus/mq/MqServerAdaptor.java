package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import io.zbus.auth.AuthResult;
import io.zbus.auth.RequestAuth;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.Session.SessionType;
import io.zbus.transport.http.HttpMessage;

public class MqServerAdaptor extends ServerAdaptor { 
	private static final Logger logger = LoggerFactory.getLogger(MqServerAdaptor.class); 
	private SubscriptionManager subscriptionManager = new SubscriptionManager();  
	private MessageDispatcher messageDispatcher;
	private MessageQueueManager mqManager = new MessageQueueManager(); 
	private RequestAuth requestAuth;
	
	private Map<String, CommandHandler> commandTable = new HashMap<>(); 
	
	public MqServerAdaptor(MqServerConfig config) {
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		mqManager.mqDir = config.mqDir;
		
		mqManager.loadQueueTable();
		
		commandTable.put(Protocol.PUB, pubHandler);
		commandTable.put(Protocol.SUB, subHandler);
		commandTable.put(Protocol.TAKE, takeHandler);
		commandTable.put(Protocol.CREATE, createHandler); 
		commandTable.put(Protocol.REMOVE, removeHandler); 
		commandTable.put(Protocol.QUERY, queryHandler); 
		commandTable.put(Protocol.PING, pingHandler); 
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		JSONObject json = null;
		SessionType sessionType = SessionType.Websocket;
		if (msg instanceof byte[]) {
			json = JSONObject.parseObject(new String((byte[])msg)); 
			sessionType = SessionType.Websocket;
		} else if (msg instanceof HttpMessage) {
			HttpMessage httpMessage = (HttpMessage)msg;
			if(httpMessage.getBody() == null){
				UrlInfo urlInfo = HttpKit.parseUrl(httpMessage.getUrl());
				json = new JSONObject();
				for(Entry<String, String> e : urlInfo.params.entrySet()) {
					json.put(e.getKey(), e.getValue());
				}
			} else {
				json = JSONObject.parseObject(httpMessage.getBodyString()); 
			}
			sessionType = SessionType.HTTP;
		} else if(msg instanceof JSONObject) {  
			json = (JSONObject)msg;
			sessionType = SessionType.Inproc; 
		} else if(msg instanceof Map) { 
			json =  new JSONObject((Map<String,Object>)msg);
			sessionType = SessionType.Inproc; 
		} else {
			throw new IllegalStateException("Not support message type");
		}
		
		sess.attr(Session.TYPE_KEY, sessionType);
		
		if (json == null) {
			reply(json, 400, "json format required", sess); 
			return;
		} 
		String cmd = (String)json.remove(Protocol.CMD);
		if (cmd == null) {
			reply(json, 400, "cmd key required", sess); 
			return;
		} 
		cmd = cmd.toLowerCase();
		
		if(requestAuth != null) {
			AuthResult authResult = requestAuth.auth(json);
			if(!authResult.success) {
				reply(json, 403, authResult.message, sess); 
				return; 
			}
		}
		
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			reply(json, 404, "Command(" + cmd + ") Not Found", sess); 
			return; 
		}
		try {
			handler.handle(json, sess);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			reply(json, 500, e.getMessage(), sess); 
			return; 
		}
	}   
	
	private CommandHandler createHandler = (req, sess) -> { 
		String mqName = req.getString(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		String mqType = req.getString(Protocol.MQ_TYPE);
		Integer mqMask = req.getInteger(Protocol.MQ_MASK); 
		String channel = req.getString(Protocol.CHANNEL); 
		Integer channelMask = req.getInteger(Protocol.CHANNEL_MASK);
		Long offset = req.getLong(Protocol.OFFSET);
		
		try {
			mqManager.saveQueue(mqName, mqType, mqMask, channel, offset, channelMask);
		} catch (IOException e) { 
			logger.error(e.getMessage(), e);
			
			reply(req, 500, e.getMessage(), sess);
			return;
		} 
		String msg = String.format("OK, CREATE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, CREATE (mq=%s)", mqName); 
		}
		reply(req, 200, msg, sess);
	};
	
	
	private CommandHandler removeHandler = (req, sess) -> { 
		String mqName = req.getString(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		String channel = req.getString(Protocol.CHANNEL);
		try {
			mqManager.removeQueue(mqName, channel);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			reply(req, 500, e.getMessage(), sess);
			return;
		}
		String msg = String.format("OK, REMOVE (mq=%s,channel=%s)", mqName, channel); 
		if(channel == null) {
			msg = String.format("OK, REMOVE (mq=%s)", mqName); 
		}
		reply(req, 200, msg, sess);
	}; 
	
	private CommandHandler pingHandler = (req, sess) -> { 
		//ignore
	};  
	
	private CommandHandler pubHandler = (req, sess) -> {
		String mqName = req.getString(Protocol.MQ);  
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		}
		
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) { 
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return; 
		} 
		
		mq.write(req); 
		Boolean ack = req.getBoolean(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, PUB (mq=%s)", mqName);
			reply(req, 200, msg, sess);
		}
		
		messageDispatcher.dispatch(mq); 
	}; 
	
	private boolean validateRequest(JSONObject req, Session sess) {
		String mqName = req.getString(Protocol.MQ);
		String channelName = req.getString(Protocol.CHANNEL);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return false;
		}
		if(channelName == null) {
			reply(req, 400, "Missing channel field", sess);
			return false;
		} 
		
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return false;
		} 
		if(mq.channel(channelName) == null) { 
			reply(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return false;
		} 
		
		return true;
	}
	
	private CommandHandler subHandler = (req, sess) -> { 
		if(!validateRequest(req, sess)) return;
		
		String mqName = req.getString(Protocol.MQ);
		String channelName = req.getString(Protocol.CHANNEL); 
		Boolean ack = req.getBoolean(Protocol.ACK); 
		if (ack == null || ack == true) {
			String msg = String.format("OK, SUB (mq=%s,channel=%s)", mqName, channelName); 
			reply(req, 200, msg, sess);
		}
		
		Integer window = req.getInteger(Protocol.WINDOW);
		Subscription sub = subscriptionManager.get(sess.id());
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id(); 
			sub.mq = mqName;
			sub.channel = channelName; 
			sub.window = window;
			subscriptionManager.add(sub);
		} else {
			sub.window = window;
		}  
		
		String topic = req.getString(Protocol.TOPIC);
		sub.topics.clear();
		if(topic != null) {
			sub.topics.add(topic); 
		}    
		MessageQueue mq = mqManager.get(mqName);
		messageDispatcher.dispatch(mq, channelName); 
	};
	
	private CommandHandler takeHandler = (req, sess) -> { 
		if(!validateRequest(req, sess)) return;
		String mqName = req.getString(Protocol.MQ);
		String channelName = req.getString(Protocol.CHANNEL); 
		Integer window = req.getInteger(Protocol.WINDOW); 
		String msgId = req.getString(Protocol.ID);
		MessageQueue mq = mqManager.get(mqName); 
		if(window == null) window = 1; 
		
	    messageDispatcher.take(mq, channelName, window, msgId, sess); 
	};
	
	private CommandHandler queryHandler = (req, sess) -> { 
		String mqName = req.getString(Protocol.MQ);
		String channelName = req.getString(Protocol.CHANNEL);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess);
			return;
		} 
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			reply(req, 404, "MQ(" + mqName + ") Not Found", sess);
			return;
		} 
		if(channelName == null) { 
			Map<String, Object> res = new HashMap<>();
			res.put(Protocol.STATUS, 200);
			res.put(Protocol.BODY, mq.info()); 
			reply(req, res, sess);
			return;
		} 
		
		ChannelInfo channel = mq.channel(channelName);
		if(channel == null) { 
			reply(req, 404, "Channel(" + channelName + ") Not Found", sess);
			return;
		}  
		
		Map<String, Object> res = new HashMap<>();
		res.put(Protocol.STATUS, 200);
		res.put(Protocol.BODY, channel); 
		reply(req, res, sess);
		return;
	};
	
	private void reply(JSONObject req, int status, String message, Session sess) {
		JSONObject res = new JSONObject();
		res.put(Protocol.STATUS, status);
		res.put(Protocol.BODY, message); 
		reply(req, res, sess);
	}
	
	private void reply(JSONObject req, Map<String, Object> res, Session sess) {
		if(req != null) {
			res.put(Protocol.ID, req.getString(Protocol.ID)); 
		}
		messageDispatcher.sendMessage(res, sess); 
	}
	 
	
	@Override
	protected void cleanSession(Session sess) throws IOException { 
		String sessId = sess.id();
		super.cleanSession(sess); 
		
		subscriptionManager.removeByClientId(sessId);
	}

	public void setRequestAuth(RequestAuth requestAuth) {
		this.requestAuth = requestAuth;
	}  
}

interface CommandHandler{
	void handle(JSONObject json, Session sess) throws IOException;
}

package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpMessage;

public class MqServerAdaptor extends ServerAdaptor { 
	private SubscriptionManager subscriptionManager = new SubscriptionManager();  
	private MessageDispatcher messageDispatcher;
	private MessageQueueManager mqManager = new MessageQueueManager(); 
	private Map<String, CommandHandler> commandTable = new HashMap<>(); 
	
	public MqServerAdaptor() {
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable); 
		
		commandTable.put(Protocol.PUB, pubHandler);
		commandTable.put(Protocol.SUB, subHandler);
		commandTable.put(Protocol.CREATE, createHandler); 
		commandTable.put(Protocol.REMOVE, removeHandler); 
		commandTable.put(Protocol.PING, pingHandler); 
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		JSONObject json = null;
		boolean isWebsocket = true;
		if (msg instanceof byte[]) {
			json = JSONObject.parseObject(new String((byte[])msg)); 
		} else if (msg instanceof HttpMessage) {
			HttpMessage httpMessage = (HttpMessage)msg;
			json = JSONObject.parseObject(httpMessage.getBodyString()); 
			isWebsocket = false;
		} else {
			throw new IllegalStateException("Not support message type");
		}
		
		String cmd = (String)json.remove(Protocol.CMD);
		if (cmd == null) {
			reply(json, 400, "cmd key required", sess, isWebsocket); 
			return;
		} 
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			reply(json, 404, "cmd="+cmd + " Not Found", sess, isWebsocket); 
			return; 
		}
		handler.handle(json, sess, isWebsocket);
	}   
	
	private CommandHandler createHandler = (req, sess, isWebsocket) -> { 
		String mqName = req.getString(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess, isWebsocket);
			return;
		}
		String mqType = req.getString(Protocol.MQ_TYPE);
		mqManager.createQueue(mqName, mqType);
		reply(req, 200, "OK", sess, isWebsocket);
	};
	
	
	private CommandHandler removeHandler = (req, sess, isWebsocket) -> { 
		String mqName = req.getString(Protocol.MQ);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess, isWebsocket);
			return;
		}
		mqManager.removeQueue(mqName);
		reply(req, 200, "OK", sess, isWebsocket);
	}; 
	
	private CommandHandler pingHandler = (req, sess, isWebsocket) -> { 
		//ignore
	}; 
	
	private CommandHandler pubHandler = (req, sess, isWebsocket) -> {
		String mqName = req.getString(Protocol.MQ);  
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess, isWebsocket);
			return;
		}
		
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) { 
			reply(req, 404, "mq="+mqName + " Not Found", sess, isWebsocket);
			return; 
		} 
		
		mq.write(req); 
		messageDispatcher.dispatch(mq); 
	};
	
	private CommandHandler subHandler = (req, sess, isWebsocket) -> { 
		String mqName = req.getString(Protocol.MQ);
		String channelName = req.getString(Protocol.CHANNEL);
		if(mqName == null) {
			reply(req, 400, "Missing mq field", sess, isWebsocket);
			return;
		}
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			reply(req, 404, "mq="+mqName + " Not Found", sess, isWebsocket);
			return;
		}
		if(channelName == null) channelName = sess.id();
		if(mq.channel(channelName) == null) { 
			reply(req, 404, "channel="+channelName + " Not Found", sess, isWebsocket);
			return;
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
		messageDispatcher.dispatch(mq, channelName); 
	};
	
	private void reply(JSONObject req, int status, String message, Session sess, boolean isWebsocket) {
		JSONObject res = new JSONObject();
		res.put(Protocol.STATUS, status);
		res.put(Protocol.DATA, message);
		res.put(Protocol.ID, req.getString(Protocol.ID)); 
		sendMessage(sess, res, isWebsocket); 
	}
	
	private void sendMessage(Session sess, JSONObject json, boolean isWebsocket) {
		if(isWebsocket) {
			sess.write(JSON.toJSONBytes(json));
			return;
		}
		
		HttpMessage res = new HttpMessage();
		Integer status = json.getInteger(Protocol.STATUS);
		if(status == null) status = 200; 
		res.setStatus(status);
		res.setJsonBody(JSON.toJSONBytes(json));
		
		sess.write(res);
	} 
	
	@Override
	protected void cleanSession(Session sess) throws IOException { 
		String sessId = sess.id();
		super.cleanSession(sess); 
		
		subscriptionManager.removeByClientId(sessId);
	} 
}

interface CommandHandler{
	void handle(JSONObject json, Session sess, boolean isWebsocket);
}

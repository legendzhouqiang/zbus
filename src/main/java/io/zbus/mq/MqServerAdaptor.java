package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.model.Channel;
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
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		JSONObject json = null;
		boolean isWebsocket = false;
		if (msg instanceof byte[]) {
			json = JSONObject.parseObject(new String((byte[])msg));
			isWebsocket = true;
		} else if (msg instanceof HttpMessage) {
			HttpMessage httpMessage = (HttpMessage)msg;
			json = JSONObject.parseObject(httpMessage.getBodyString()); 
			isWebsocket = false;
		} else {
			throw new IllegalStateException("Not support message type");
		}
		
		String cmd = (String)json.remove(Protocol.CMD);
		if (cmd == null) {
			JSONObject res = new JSONObject();
			res.put(Protocol.STATUS, 400);
			res.put(Protocol.DATA, "cmd key required");
			res.put(Protocol.ID, json.getString(Protocol.ID));

			sendMessage(sess, json, isWebsocket);
			return;
		} 
		CommandHandler handler = commandTable.get(cmd);
		if(handler == null) {
			JSONObject res = new JSONObject();
			res.put(Protocol.STATUS, 404);
			res.put(Protocol.DATA, "cmd="+cmd + " not found");
			res.put(Protocol.ID, json.getString(Protocol.ID)); 
			sendMessage(sess, json, isWebsocket);
			return;
		}
		handler.handle(json, sess, isWebsocket);
	} 
	
	@Override
	protected void cleanSession(Session sess) throws IOException { 
		String sessId = sess.id();
		super.cleanSession(sess); 
		
		subscriptionManager.removeByClientId(sessId);
	} 
	
	private CommandHandler createHandler = (json, sess, isWebsocket) -> {
		String mqName = json.getString(Protocol.MQ);  
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) {
			mq = mqManager.create(mqName);
		}
		
		mq.write(json); 
		messageDispatcher.dispatch(mq); 
	};
	
	private CommandHandler pubHandler = (json, sess, isWebsocket) -> {
		String mqName = json.getString(Protocol.MQ);  
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) {
			mq = mqManager.create(mqName);
		} 
		mq.write(json); 
		messageDispatcher.dispatch(mq); 
	};
	
	private CommandHandler subHandler = (json, sess, isWebsocket) -> {
		Subscription sub = subscriptionManager.get(sess.id());
		String mqName = json.getString(Protocol.MQ);
		String channelName = json.getString(Protocol.CHANNEL);
		if(mqName == null) mqName = "";
		if(channelName == null) channelName = sess.id();
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id(); 
			sub.mq = mqName;
			sub.channel = channelName;
			
			subscriptionManager.add(sub);
		}  
		
		String topic = json.getString(Protocol.TOPIC);
		sub.topics.clear();
		if(topic != null) {
			sub.topics.add(topic); 
		}
		
		MessageQueue mq = mqManager.get(mqName); 
		if(mq == null) {
			mq = mqManager.create(mqName);
		}
		
		if(mq.channel(channelName) == null) { 
			Channel channel = new Channel();
			channel.name = channelName;
			mq.saveChannel(channel);
		}
		messageDispatcher.dispatch(mq, channelName); 
	};
	
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
}

interface CommandHandler{
	void handle(JSONObject json, Session sess, boolean isWebsocket);
}

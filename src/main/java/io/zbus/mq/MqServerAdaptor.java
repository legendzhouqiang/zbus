package io.zbus.mq;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageDispatcher;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.MqManager;
import io.zbus.mq.model.Subscription;
import io.zbus.mq.model.SubscriptionManager;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpMessage;

public class MqServerAdaptor extends ServerAdaptor { 
	private SubscriptionManager subscriptionManager = new SubscriptionManager();  
	private MessageDispatcher messageDispatcher;
	private MqManager mqManager = new MqManager();
	
	
	public MqServerAdaptor() {
		messageDispatcher = new MessageDispatcher(subscriptionManager, sessionTable);
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		JSONObject json = null;
		if (msg instanceof byte[]) {
			json = JSONObject.parseObject(new String((byte[])msg));
		} else if (msg instanceof HttpMessage) {
			HttpMessage httpMessage = (HttpMessage)msg;
			json = JSONObject.parseObject(httpMessage.getBodyString()); 
		} else {
			throw new IllegalStateException("Not support message type");
		}
		
		handleJsonMessage(json, sess);
	} 
	
	@Override
	protected void cleanSession(Session sess) throws IOException { 
		String sessId = sess.id();
		super.cleanSession(sess); 
		
		subscriptionManager.removeByClientId(sessId);
	}
	
	protected void handleJsonMessage(JSONObject json, Session sess) throws IOException { 
		String cmd = (String)json.remove(Protocol.CMD);
		if (cmd == null) {
			JSONObject res = new JSONObject();
			res.put(Protocol.STATUS, 400);
			res.put(Protocol.DATA, "cmd key required");
			res.put(Protocol.ID, json.getString(Protocol.ID));

			sendMessage(sess, json, true);
			return;
		}

		
		if (cmd.equals(Protocol.PUB)) {
			
			handlePubMessage(json, sess);  
			
		} else if (cmd.equals(Protocol.SUB)) {
			
			handleSubMessage(json, sess); 
		} 
	} 
	
	private void handlePubMessage(JSONObject json, Session sess) throws IOException {
		String mqName = json.getString(Protocol.MQ);  
		MessageQueue mq = mqManager.get(mqName);
		if(mq == null) {
			mq = mqManager.create(mqName);
		}
		
		mq.write(json); 
		messageDispatcher.dispatch(mq); 
	}
	
	private void handleSubMessage(JSONObject json, Session sess) throws IOException {
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

}

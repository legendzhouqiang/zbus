package io.zbus.mq;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session;
import io.zbus.net.http.HttpMessage;

public class MqServerAdaptor extends ServerAdaptor { 
	private Map<String, Subscription> subscriptionTable = new ConcurrentHashMap<>();
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
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
		
		subscriptionTable.remove(sessId);
	}
	
	protected void handleJsonMessage(JSONObject json, Session sess) throws IOException { 
		String cmd = json.getString(Protocol.CMD);
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
		String topic = json.getString(Protocol.TOPIC);
		for(Entry<String, Subscription> e : subscriptionTable.entrySet()) {
			Subscription sub = e.getValue();
			if(sub.topics.contains(topic)) {
				Session matched = sessionTable.get(sub.clientId);
				if(matched != null) { 
					json.remove(Protocol.CMD);
					sendMessage(matched, json, true); 
				}
			}
		} 
	}
	
	private void handleSubMessage(JSONObject json, Session sess) throws IOException {
		Subscription sub = subscriptionTable.get(sess.id()); 
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id();
			subscriptionTable.put(sub.clientId, sub);
		} 
		
		String topic = json.getString(Protocol.TOPIC);
		sub.topics.clear();
		sub.topics.add(topic);
		
		String mqName = json.getString(Protocol.MQ);
		if(mqName == null) {
			mqName = ""; //empty name
		}
		
		MessageQueue mq = mqTable.get(mqName);
		if(mq == null) { 
			// mq: { name: xx }
			// channel: { name: xxx }
		}
		
		String channel = json.getString(Protocol.CHANNEL);
		if(channel == null) {
			
		}
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

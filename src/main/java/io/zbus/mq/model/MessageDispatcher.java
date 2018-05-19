package io.zbus.mq.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.Protocol;
import io.zbus.transport.Session;
import io.zbus.transport.http.Message;

public class MessageDispatcher {  
	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;   
	private Map<String, Long> channelIndexTable = new ConcurrentHashMap<String, Long>();
	
	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable; 
	} 
	
	@SuppressWarnings("unchecked")
	public void dispatch(MessageQueue mq, String channel) {   
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if(subs == null || subs.size() == 0) return;
		Long index = channelIndexTable.get(channel);
		if(index == null) {
			index = 0L; 
		}
		int count = 10;
	    List<Object> messages = mq.read(channel, count); 
		while(true) {
			for(Object message : messages) {
				if(!(message instanceof Map)) continue;
				Map<String, Object> data = (Map<String, Object>)message;
				String topic = (String)data.get(Protocol.TOPIC);
				int N = subs.size();
				long max = index+N;
				while(index<max) {
					Subscription sub = subs.get((int)(index%N));
					index++;
					if (index < 0) index = 0L;
					if(sub.topics.isEmpty() || sub.topics.contains(topic)) {
						Session sess = sessionTable.get(sub.clientId);
						if(sess == null) continue;
						sendMessage(sess, data, sub.isWebsocket); 
						break;
					}
				} 
			}
			if(messages.size() < count) break;
		}
		channelIndexTable.put(channel, index);
	}
	
	public void dispatch(MessageQueue mq) {
		for(Channel channel : mq.channels().values()) {
			dispatch(mq, channel.name);
		}
	}
	
	private void sendMessage(Session sess, Map<String, Object> data, boolean isWebsocket) {
		if(isWebsocket) {
			sess.write(JSON.toJSONBytes(data));
			return;
		}
		
		Message res = new Message();
		Integer status = (Integer)data.get(Protocol.STATUS);
		if(status == null) status = 200; 
		res.setStatus(status);
		res.setJsonBody(JSON.toJSONBytes(data));
		
		sess.write(res);
	}
}

package io.zbus.mq.model;

import java.util.List;
import java.util.Map;

import io.zbus.mq.Protocol;
import io.zbus.net.Session;

public class MessageDispatcher {  
	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;   
	
	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable; 
	} 
	
	@SuppressWarnings("unchecked")
	public void dispatch(MessageQueue mq, String channel) {   
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if(subs.size() == 0) return;
		
		int count = 10;
	    List<Object> messages = mq.read(channel, count);
		while(true) {
			for(Object message : messages) {
				if(!(message instanceof Map)) continue;
				Map<String, Object> data = (Map<String, Object>)message;
				String topic = (String)data.get(Protocol.TOPIC);
				for(Subscription sub : subs) {
					if(sub.topics.contains(topic)) {
						 
					}
				}
			}
			if(messages.size() < count) break;
		}
	}
	
	public void dispatch(MessageQueue mq) {
		for(Channel channel : mq.channels().values()) {
			dispatch(mq, channel.name);
		}
	}
}

package io.zbus.mq.model;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.Protocol;
import io.zbus.net.Session;
import io.zbus.net.http.HttpMessage;

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
						Session sess = sessionTable.get(sub.clientId);
						if(sess == null) continue;
						sendMessage(sess, data, sub.isWebsocket);
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
	
	private void sendMessage(Session sess, Map<String, Object> data, boolean isWebsocket) {
		if(isWebsocket) {
			sess.write(JSON.toJSONBytes(data));
			return;
		}
		
		HttpMessage res = new HttpMessage();
		Integer status = (Integer)data.get(Protocol.STATUS);
		if(status == null) status = 200; 
		res.setStatus(status);
		res.setJsonBody(JSON.toJSONBytes(data));
		
		sess.write(res);
	}
}

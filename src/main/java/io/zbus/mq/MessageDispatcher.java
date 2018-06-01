package io.zbus.mq;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpMessage;

public class MessageDispatcher {  
	private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class); 
	
	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;   
	private Map<String, Long> channelIndexTable = new ConcurrentHashMap<String, Long>(); 
	
	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable; 
	} 
	 
	
	public void dispatch(MessageQueue mq, String channel) {   
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if(subs == null || subs.size() == 0) return;
		
		synchronized (subs) {
			Long index = channelIndexTable.get(channel);
			if(index == null) {
				index = 0L; 
			}
			int count = 10; 
			while(true) {
				List<Map<String, Object>> data;
				try {
					data = mq.read(channel, count);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					break;
				} 
				for(Map<String, Object> message : data) { 
					String topic = (String)message.get(Protocol.TOPIC);
					int N = subs.size();
					long max = index+N;
					while(index<max) {
						Subscription sub = subs.get((int)(index%N));
						index++;
						if (index < 0) index = 0L;
						if(sub.topics.isEmpty() || sub.topics.contains(topic)) {
							Session sess = sessionTable.get(sub.clientId);
							if(sess == null) continue;
							message.put(Protocol.CHANNEL, channel);
							message.put(Protocol.SENDER, sess.id()); 
							sendMessage(sess, message, sub.isWebsocket); 
							break;
						}
					} 
				}
				if(data.size() < count) break;
			}
			channelIndexTable.put(channel, index);
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

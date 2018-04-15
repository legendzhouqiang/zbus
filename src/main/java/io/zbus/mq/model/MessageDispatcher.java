package io.zbus.mq.model;

import java.util.List;
import java.util.Map;

import io.zbus.net.Session;

public class MessageDispatcher {  
	protected Map<String, Session> sessionTable;
	protected Map<String, MessageQueue> mqTable;
	protected int readBatchSize = 64;
	
	public MessageDispatcher(Map<String, Session> sessionTable, Map<String, MessageQueue> mqTable) {
		this.sessionTable = sessionTable;
		this.mqTable = mqTable;
	} 
	
	public void dispatch(Channel channel) { 
		MessageQueue mq = mqTable.get(channel.queue);
		if(mq == null) return;
		
		List<Object> messages;
		do {
			messages = mq.read(channel.id, readBatchSize);
		} while(messages != null && messages.size() == readBatchSize); 
	}
	
	public void dispatch(MessageQueue mq) {
		
	}
}

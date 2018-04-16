package io.zbus.mq.model;

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
	}
	
	public void dispatch(MessageQueue mq) {
		
	}
}

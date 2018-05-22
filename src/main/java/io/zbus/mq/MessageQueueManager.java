package io.zbus.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.memory.MemoryQueue;

public class MessageQueueManager {
	public static final String MEMORY = "mem";
	public static final String DISK = "disk";
	public static final String DB = "db";
	
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	public MessageQueue createQueue(String mqName, String mqType) { 
		if(mqName == null) {
			throw new IllegalArgumentException("Missing mqName");
		}
		if(mqType == null) {
			mqType = MEMORY;
		}
		MessageQueue mq = new MemoryQueue(mqName);
		mqTable.put(mqName, mq);
		return mq;
	} 
	
	public void removeQueue(String mqName) {
		mqTable.remove(mqName);
	}
	
	public void createChannel(String mqName, String channelId) {
		
	}
	
	public void removeChannel(String mqName, String channelId) {
		
	}
}

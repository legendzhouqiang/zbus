package io.zbus.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.memory.MemoryQueue;

public class MessageQueueManager {
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	public MessageQueue create(String mqName) {
		if(mqName == null) mqName = "";
		MessageQueue mq = new MemoryQueue(mqName);
		mqTable.put(mqName, mq);
		return mq;
	} 
}

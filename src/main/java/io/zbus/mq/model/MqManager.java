package io.zbus.mq.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.memory.MemoryQueue;

public class MqManager {
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

package io.zbus.mq.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MqManager {
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public MessageQueue get(String mqName) {
		return mqTable.get(mqName);
	} 
	
}

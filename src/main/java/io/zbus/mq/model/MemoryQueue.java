package io.zbus.mq.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryQueue implements MessageQueue{ 
	private long maxCount = 1000; 
	
	private List<Object> messages = Collections.synchronizedList(new LinkedList<>());
	
	
	private Domain domain;
	private Map<String, Channel> channelTable = new ConcurrentHashMap<>();
	
	
	
	public MemoryQueue(String domain) {
		this.domain = new Domain();
		this.domain.name = domain;
	}
	
	public MemoryQueue(Domain domain) {
		this.domain = domain;
	}

	@Override
	public Domain getDomain() { 
		return this.domain;
	}

	@Override
	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	@Override
	public Channel getChannel(String channelId) {
		return channelTable.get(channelId); 
	}

	@Override
	public void addChannel(Channel channel) { 
		channelTable.put(channel.id, channel);
	}

	@Override
	public void removeChannel(String channelId) { 
		channelTable.remove(channelId);
		
	}

	@Override
	public void updateChannel(Channel channel) {
		channelTable.put(channel.id, channel);
	}

	@Override
	public void write(Object message) { 
		if(messages.size() > maxCount) {
			messages.remove(0);
		}
		messages.add(message);
	}

	@Override
	public Object read(String channelId) { 
		Channel channel = channelTable.get(channelId);
		if(channel == null) {
			throw new IllegalArgumentException("Missing channel: " + channelId);
		} 
		return null;
	}
	
	@Override
	public List<Object> read(String channelId, int batchSize) { 
		return null;
	}

}

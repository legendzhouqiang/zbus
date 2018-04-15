package io.zbus.mq.memory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class MemoryQueue implements MessageQueue{ 
	private long maxCount = 1000; 
	
	private List<Object> messages = Collections.synchronizedList(new LinkedList<>());
	
	private String name;
	
	private Map<String, Channel> channelTable = new ConcurrentHashMap<>(); 
	
	public MemoryQueue(String name) { 
		this.name = name;
	} 
  
	public String getName() {
		return name;
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

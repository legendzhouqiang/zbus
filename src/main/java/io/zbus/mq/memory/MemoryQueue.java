package io.zbus.mq.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.memory.CircularArray.Reader;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class MemoryQueue implements MessageQueue{  
	private CircularArray data;  
	private String name; 
	private Map<String, Channel> channelTable = new ConcurrentHashMap<>(); 
	private Map<String, Reader> readerTable = new ConcurrentHashMap<>(); 
	private Map<String, Object> attributes = new ConcurrentHashMap<>();
	
	public MemoryQueue(String name, int maxSize) { 
		this.name = name;
		this.data = new CircularArray(maxSize);
	}  
	
	public MemoryQueue(String name) { 
		this(name, 1000);
	} 
	 
  
	public String name() {
		return name;
	}
	
	@Override
	public Map<String, Object> attributes() { 
		return attributes;
	}
	
	@Override
	public void flush() {  
		
	}
	 
	@Override
	public Channel channel(String channelId) {
		return channelTable.get(channelId).clone(); 
	}

	@Override
	public void saveChannel(Channel channel) {  
		channel = channel.clone(); //clone it 
		Reader reader = readerTable.get(channel.name);
		if(reader == null) {
			reader = data.createReader(channel);
			readerTable.put(channel.name, reader);
		}
		reader.setOffset(channel.offset); 
		channelTable.put(channel.name, channel);  
	} 

	@Override
	public void removeChannel(String channelId) { 
		channelTable.remove(channelId); 
		readerTable.remove(channelId);
	} 
	
	@Override
	public void write(Object... message) {  
		data.write(message);
	} 
	
	@Override
	public List<Object> read(String channelId, int count) { 
		Reader reader = readerTable.get(channelId);
		if(reader == null) {
			throw new IllegalArgumentException("Missing channel: " + channelId);
		}   
		return reader.read(count);
	} 
}

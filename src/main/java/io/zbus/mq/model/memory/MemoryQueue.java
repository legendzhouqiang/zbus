package io.zbus.mq.model.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.memory.CircularArray.Reader;

public class MemoryQueue implements MessageQueue{  
	private CircularArray data;  
	private String name; 
	private Long mask;
	private Map<String, Channel> channelTable = new ConcurrentHashMap<>(); 
	private Map<String, Reader> readerTable = new ConcurrentHashMap<>();  
	
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
	public Long mask() { 
		return mask;
	}
	
	@Override
	public void flush() {  
		
	}
	 
	@Override
	public Channel channel(String channelId) {
		Channel channel = channelTable.get(channelId);
		if(channel == null) return null;
		return channel.clone(); 
	}
	
	@Override
	public Map<String, Channel> channels() { 
		return channelTable;
	}

	@Override
	public void addChannel(Channel channel) {  
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
	
	@Override
	public void destroy() { 
		
	}
}

package io.zbus.mq.memory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class MemoryQueue implements MessageQueue{  
	private CircularArray data;  
	private final String name; 
	private Integer mask;
	private Map<String, Channel> channelTable = new ConcurrentHashMap<>(); 
	private Map<String, MemoryChannelReader> readerTable = new ConcurrentHashMap<>();  
	
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
	public Integer getMask() { 
		return mask;
	}
	
	@Override
	public void setMask(Integer mask) {
		this.mask = mask;
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
	public void saveChannel(Channel channel) throws IOException {  
		channel = channel.clone(); //clone it 
		MemoryChannelReader reader = readerTable.get(channel.name);
		if(reader == null) {
			reader = new MemoryChannelReader(data, channel);
			readerTable.put(channel.name, reader);
		}
		reader.seek(channel.offset, null); //TODO msgId to validate
		channelTable.put(channel.name, channel);  
	} 

	@Override
	public void removeChannel(String channelId) { 
		channelTable.remove(channelId); 
		readerTable.remove(channelId);
	} 
	
	@Override
	public void write(Map<String, Object> message) {  
		data.write(message);
	} 
	
	@Override
	public void write(List<Map<String, Object>> messages) { 
		data.write(messages.toArray());
	}
	
	@Override
	public List<Map<String, Object>> read(String channelId, int count) throws IOException { 
		MemoryChannelReader reader = readerTable.get(channelId);
		if(reader == null) {
			throw new IllegalArgumentException("Missing channel: " + channelId);
		}   
		return reader.read(count);
	} 
	
	@Override
	public Map<String, Object> read(String channelId) throws IOException {
		MemoryChannelReader reader = readerTable.get(channelId);
		if(reader == null) {
			throw new IllegalArgumentException("Missing channel: " + channelId);
		}   
		return reader.read();
	}
	
	@Override
	public void destroy() { 
		
	}
}

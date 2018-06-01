package io.zbus.mq.memory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.ChannelReader;
import io.zbus.mq.model.MessageQueue.AbstractMessageQueue;

public class MemoryQueue extends AbstractMessageQueue{  
	private CircularArray data;   
	private Integer mask; 
	
	public MemoryQueue(String name, int maxSize) { 
		super(name); 
		this.data = new CircularArray(maxSize); 
	}  
	
	public MemoryQueue(String name) { 
		this(name, 1000); 
	} 
	
	@Override
	protected ChannelReader buildChannelReader(String channelId) throws IOException {
		Channel channel = new Channel(channelId);
		return new MemoryChannelReader(data, channel);
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
	public void destroy() { 
		
	}
}

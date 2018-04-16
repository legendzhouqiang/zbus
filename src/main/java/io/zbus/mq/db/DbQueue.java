package io.zbus.mq.db;

import java.util.List;
import java.util.Map;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class DbQueue implements MessageQueue{

	@Override
	public String name() {
		return null;
	}
	
	@Override
	public Map<String, Object> attributes() {
		return null;
	}
	 
	@Override
	public void flush() {
		
	}

	@Override
	public Channel channel(String channelId) {
		return null;
	}

	@Override
	public void saveChannel(Channel channel) {
		
	}

	@Override
	public void removeChannel(String channelId) {
		
	}  

	@Override
	public List<Object> read(String channelId, int count) {
		return null;
	}

	@Override
	public void write(Object... message) { 
		
	}  
}

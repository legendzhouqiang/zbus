package io.zbus.mq.model.db;

import java.util.List;
import java.util.Map;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class DbQueue implements MessageQueue{
	
	public DbQueue(String mqName) { 
	}
	
	@Override
	public String name() {
		return null;
	}

	@Override
	public Long mask() {
		// TODO Auto-generated method stub
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
	public Map<String, Channel> channels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChannel(Channel channel) {
		
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
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}

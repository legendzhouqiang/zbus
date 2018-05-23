package io.zbus.mq.model.disk;

import java.util.List;
import java.util.Map;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class DiskQueue implements MessageQueue {

	public DiskQueue(String mqName) { 
	}
	
	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(Object... message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Object> read(String channelId, int count) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel channel(String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChannel(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeChannel(String channelId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Channel> channels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long mask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}

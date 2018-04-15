package io.zbus.mq.db;

import java.util.List;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class DbQueue implements MessageQueue{

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel getChannel(String channelId) {
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
	public void updateChannel(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(Object message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object read(String channelId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> read(String channelId, int batchSize) {
		// TODO Auto-generated method stub
		return null;
	} 
	 

}

package org.zbus.server.mq.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.server.mq.MessageQueue;
 
public class MessageStoreDummy implements MessageStore {
	private static final Logger log = LoggerFactory.getLogger(MessageStoreDummy.class);
	
	@Override
	public void saveMessage(Message message) { 
		if(log.isDebugEnabled()){
			log.debug("Dummy save: "+ message);
		}
	}

	@Override
	public void removeMessage(Message message) {  
		if(log.isDebugEnabled()){
			log.debug("Dummy remove: "+ message);
		}
	}
	
	@Override
	public void onMessageQueueCreated(MessageQueue mq) { 
		
	}
	
	@Override
	public void onMessageQueueRemoved(MessageQueue mq) { 
		
	}
	
	@Override
	public ConcurrentMap<String, MessageQueue> loadMqTable() { 
		return new ConcurrentHashMap<String, MessageQueue>();
	}
	
	@Override
	public void start() { 
	}
	
	@Override
	public void shutdown() { 
		
	}
}

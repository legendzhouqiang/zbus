package org.zstacks.zbus.server.mq.store;

import java.util.concurrent.ConcurrentMap;

import org.zstacks.zbus.server.mq.MessageQueue;
import org.zstacks.znet.Message;

public interface MessageStore {
	void saveMessage(Message message);
	void removeMessage(Message message); 
	void onMessageQueueCreated(MessageQueue mq);
	void onMessageQueueRemoved(MessageQueue mq);
	
	ConcurrentMap<String, MessageQueue> loadMqTable() throws Exception;
	
	void start() throws Exception;
	void shutdown() throws Exception;
}

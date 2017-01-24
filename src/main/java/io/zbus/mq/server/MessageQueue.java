
package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.ConsumerGroup;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.net.Session;

public interface MessageQueue { 

	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException;   
	
	void declareConsumerGroup(ConsumerGroup ctrl) throws Exception;
	
	long remaining(String consumeGroup);
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);
	
	void cleanSession();
	
	TopicInfo getTopicInfo();
	
	String getCreator();

	void setCreator(String value); 
	
	int getFlag();

	void setFlag(int value); 
	
	long getLastUpdateTime();
	
	String getName();
}

package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.net.Session;

public interface MessageQueue { 

	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException;   
	
	ConsumeGroupInfo declareConsumeGroup(ConsumeGroup consumeGroup) throws Exception;
	
	void removeConsumeGroup(String groupName) throws IOException; 
	
	void remove() throws IOException;
	
	long remaining(String consumeGroup);
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);
	
	void cleanSession();
	
	TopicInfo topicInfo();
	
	long lastUpdateTime();
	
	String name();
	
	String getCreator();

	void setCreator(String value); 
	
	int getFlag();

	void setFlag(int value);  
}

package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.net.Session;
import io.zbus.net.http.Message;

public interface MessageQueue { 

	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException;  
	
	MessageQueue childMessageQueue();
	
	void declareConsumeGroup(ConsumeGroup ctrl) throws Exception;
	
	long remaining(String consumeGroup);
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);
	
	void cleanSession();
	
	MqInfo getMqInfo();
	
	String getCreator();

	void setCreator(String value); 
	
	int getFlag();

	void setFlag(int value); 
	
	long getLastUpdateTime();
	
	String getName();
}
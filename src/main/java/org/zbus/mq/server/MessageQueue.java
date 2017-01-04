
package org.zbus.mq.server;

import java.io.IOException;

import org.zbus.mq.ConsumeGroup;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.net.Session;
import org.zbus.net.http.Message;

public interface MessageQueue { 

	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException;  
	
	void declareConsumeGroup(ConsumeGroup ctrl) throws Exception;
	
	int remaining(String consumeGroup);
	
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
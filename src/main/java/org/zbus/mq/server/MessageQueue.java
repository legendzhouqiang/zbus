
package org.zbus.mq.server;

import java.io.IOException;

import org.zbus.mq.Protocol.MqInfo;
import org.zbus.net.Session;
import org.zbus.net.http.Message;

public interface MessageQueue {
	
	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException; 
	
	int remaining(String readerGroup);
	
	int consumerCount(String readerGroup); 
	
	void cleanSession(Session sess);
	
	void cleanSession();
	
	MqInfo getMqInfo();
	
	String getCreator();

	void setCreator(String value);

	String getAccessToken();

	void setAccessToken(String value); 
	
	int getFlag();

	void setFlag(int value); 
	
	long getLastUpdateTime();
	
	String getName();
}
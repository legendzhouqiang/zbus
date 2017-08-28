
package io.zbus.mq;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.transport.Session;

public interface MessageQueue { 

	void produce(Message message) throws IOException; 
	
	Message consume(String consumeGroup, Integer window) throws IOException;  
	
	void consume(Message message, Session session) throws IOException;  
	
	ConsumeGroupInfo declareGroup(ConsumeGroup consumeGroup) throws Exception;
	
	ConsumeGroupInfo consumeGroup(String groupName);
	
	void removeGroup(String groupName) throws IOException; 
	
	void removeTopic() throws IOException; 
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);  
	
	String getTopic();
	
	TopicInfo getInfo();
	
	long getUpdateTime();  
	
	String getCreator();

	void setCreator(String value); 
	
	int getMask();

	void setMask(int value);  
}


class PullSession { 
	Session session;
    Message pullMessage;  
   
    final ReentrantLock lock = new ReentrantLock(); 
	final BlockingQueue<Message> msgQ = new LinkedBlockingQueue<Message>(); 
	
	public PullSession(Session sess, Message pullMessage) { 
		this.session = sess;
		this.setPullMessage(pullMessage);
	}  
	public Session getSession() {
		return session;
	}
	
	public void setSession(Session session) {
		this.session = session;
	}
	
	public Message getPullMessage() {
		return this.pullMessage;
	}
	
	public void setPullMessage(Message msg) { 
		this.lock.lock();
		this.pullMessage = msg;
		if(msg == null){
			this.lock.unlock();
			return; 
		} 
		this.lock.unlock();
	}  

	public BlockingQueue<Message> getMsgQ() {
		return msgQ;
	}
	
	public String getConsumerAddress(){
		return session.remoteAddress();   
	}
}
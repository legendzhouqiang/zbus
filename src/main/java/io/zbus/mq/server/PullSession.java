package io.zbus.mq.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.mq.Message;
import io.zbus.net.Session;

public class PullSession { 
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
		return session.getRemoteAddress();   
	}
}


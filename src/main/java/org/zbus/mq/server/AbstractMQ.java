package org.zbus.mq.server;
 
import java.io.IOException;
import java.util.AbstractQueue;

import org.zbus.mq.Protocol.MqInfo;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public abstract class AbstractMQ{	
	protected final String name;
	protected String creator;
	protected long lastUpdateTime = System.currentTimeMillis();
	
	protected final AbstractQueue<Message> msgQ;
	
	public AbstractMQ(String name, AbstractQueue<Message> msgQ) {
		this.msgQ = msgQ;
		this.name = name;
	}
	 
	public String getName() { 
		return name;
	}
 
	public void produce(Message msg, Session sess) throws IOException { 
		msgQ.offer(msg); 
		dispatch();
	} 
 
	public abstract void consume(Message msg, Session sess) throws IOException;

	abstract void dispatch() throws IOException; 
	
	public abstract void cleanSession();
	
	public abstract MqInfo getMqInfo();
}

package org.zbus.mq.server;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.log.Logger;
import org.zbus.mq.Protocol.ConsumerInfo;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public class MQ extends AbstractMQ{
	private static final Logger log = Logger.getLogger(MQ.class);  
	
	protected final BlockingQueue<PullSession> pullQ = new LinkedBlockingQueue<PullSession>();
	
	public MQ(String name, AbstractQueue<Message> msgQ) {
		super(name, msgQ);
	}
	
	@Override
	public void consume(Message msg, Session sess) throws IOException {  
		for(PullSession pull : pullQ){
			if(pull.getSession() == sess){
				pull.setPullMessage(msg);
				this.dispatch();
				return; 
			}
		} 
		
		PullSession pull = new PullSession(sess, msg);
		pullQ.offer(pull);  
		this.dispatch();
	}
	
	void dispatch() throws IOException{  
		while(pullQ.peek() != null && msgQ.size() > 0){
			PullSession pull = pullQ.poll(); 
			if(pull == null){
				continue;
			}
			if( !pull.getSession().isActive() ){ 
				continue;
			} 
			
			Message msg = msgQ.poll();
			if(msg == null){
				continue;
			} 
			this.lastUpdateTime = System.currentTimeMillis();
			
			try {  
				Message pullMsg = pull.getPullMessage(); 
				Message writeMsg = Message.copyWithoutBody(msg);
				
				writeMsg.setRawId(msg.getId());  //保留原始消息ID
				writeMsg.setId(pullMsg.getId()); //配对订阅消息！
				
				pull.getSession().write(writeMsg); 
			
			} catch (IOException ex) {   
				log.error(ex.getMessage(), ex); 
				msgQ.offer(msg);
			} 
		} 
	}
	
	@Override
	public void cleanSession() { 
		Iterator<PullSession> iter = pullQ.iterator();
		while(iter.hasNext()){
			PullSession pull = iter.next();
			if(!pull.session.isActive()){
				iter.remove();
			}
		}
	}
	
	@Override
	public MqInfo getMqInfo() { 
		MqInfo info = new MqInfo(); 
		info.name = name;
		info.lastUpdateTime = lastUpdateTime;
		info.creator = creator;
		info.mode = MqMode.MQ.intValue();
		info.unconsumedMsgCount = msgQ.size();
		info.consumerInfoList = new ArrayList<ConsumerInfo>();
		for(PullSession pull : pullQ){ 
			info.consumerInfoList.add(pull.getConsumerInfo());
		} 
		return info;
	}
}

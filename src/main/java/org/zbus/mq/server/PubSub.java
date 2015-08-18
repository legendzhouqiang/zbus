package org.zbus.mq.server;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.zbus.log.Logger;
import org.zbus.mq.Protocol.ConsumerInfo;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public class PubSub extends AbstractMQ{
	private static final Logger log = Logger.getLogger(PubSub.class);  
	
	protected final ConcurrentMap<String, PullSession> pullMap = new ConcurrentHashMap<String, PullSession>(); 
	
	public PubSub(String name, AbstractQueue<Message> msgQ){
		super(name, msgQ);
	}
	 
	@Override
	public void consume(Message msg, Session sess) throws IOException {  
		PullSession pull = pullMap.get(sess.id());
		if(pull != null){
			pull.setPullMessage(msg); 
		} else {
			pull = new PullSession(sess, msg); 
			pullMap.putIfAbsent(sess.id(), pull);
		} 
		this.dispatch();
	}
	
	void dispatch() throws IOException{  
		Message msg = null;
		while((msg = msgQ.poll()) != null){
			this.lastUpdateTime = System.currentTimeMillis();
			
			String topic = msg.getTopic();
			Iterator<Entry<String, PullSession>> iter = pullMap.entrySet().iterator();
			while(iter.hasNext()){
				PullSession sess = iter.next().getValue();
				if(sess == null || !sess.getSession().isActive()){
					iter.remove();
					continue;
				} 
				if(sess.isTopicMatched(topic)){ 
					Message copy = Message.copyWithoutBody(msg);
					copy.setStatus(200);
					sess.getMsgQ().offer(copy);
				}
			}
		} 
	 
		Iterator<Entry<String, PullSession>> iter = pullMap.entrySet().iterator();
		while(iter.hasNext()){
			PullSession pull = iter.next().getValue();
			if(pull == null || !pull.getSession().isActive()){
				iter.remove();
				continue;
			}  
			try{ 
				pull.lock.lock();
				Message pullMessage = pull.pullMessage;
				if(pullMessage == null) continue;
				
				msg = pull.getMsgQ().poll();
				if(msg == null) break;
				
				msg.setStatus(200); //支持浏览器
				msg.setRawId(msg.getId());
				msg.setId(pullMessage.getId()); 
				
				pull.getSession().write(msg);
			} catch(IOException ex){
				log.error(ex.getMessage(), ex);
			} finally{
				pull.lock.unlock();
			}
		}  
	}
	
	@Override
	public void cleanSession() { 
		Iterator<Entry<String, PullSession>> iter = pullMap.entrySet().iterator();
		while(iter.hasNext()){
			PullSession pull = iter.next().getValue();
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
		info.mode = MqMode.PubSub.intValue();
		info.unconsumedMsgCount = msgQ.size();
		info.consumerInfoList = new ArrayList<ConsumerInfo>();
		for(PullSession pull : pullMap.values()){ 
			info.consumerInfoList.add(pull.getConsumerInfo());
		} 
		return info;
	}
}

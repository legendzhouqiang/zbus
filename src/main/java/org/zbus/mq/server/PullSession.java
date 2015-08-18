package org.zbus.mq.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.zbus.mq.Protocol.ConsumerInfo;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public class PullSession { 
	Session session;
    Message pullMessage;  
   
    final ReentrantLock lock = new ReentrantLock();
	final Set<String> topicSet = new HashSet<String>(); 
	final BlockingQueue<Message> msgQ = new LinkedBlockingQueue<Message>(); 
	
	public PullSession(Session sess, Message pullMessage) { 
		this.session = sess;
		this.setPullMessage(pullMessage);
	}
	
	public void subscribeTopics(String topicString){
		if(topicString == null) return;  
		String[] ts = topicString.split("[,]");
		for(String t : ts){
			if(t.trim().length() == 0) continue;
			topicSet.add(t.trim());
		}
	}
	
	public boolean isTopicMatched(String topic){
		if(topic == null) return false;  
		if(topicSet.contains("*")) return true;
		return topicSet.contains(topic);
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
		this.pullMessage = msg;
		if(msg == null) return; 
		String topic = this.pullMessage.getTopic();
		if(topic != null){
			this.subscribeTopics(topic);
		}
	} 
	
	public Set<String> getTopics(){
		return this.topicSet;
	}

	public BlockingQueue<Message> getMsgQ() {
		return msgQ;
	}
	
	public ConsumerInfo getConsumerInfo(){
		ConsumerInfo info = new ConsumerInfo();
		info.remoteAddr = session.getRemoteAddress();
		info.status = ""+session.getStatus();
		info.topics = topicSet;
		return info;
	}
}


/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.zbus.mq.Protocol.ConsumerInfo;
import org.zbus.net.Session;
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
		topicSet.clear();
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
		//info.status = ""+session.getStatus(); FIXME
		info.topics = topicSet;
		return info;
	}
}


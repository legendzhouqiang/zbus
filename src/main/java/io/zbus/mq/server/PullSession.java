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
package io.zbus.mq.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol.ConsumerInfo;
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
	
	public ConsumerInfo getConsumerInfo(){
		ConsumerInfo info = new ConsumerInfo();
		info.remoteAddr = session.getRemoteAddress();  
		return info;
	}
}


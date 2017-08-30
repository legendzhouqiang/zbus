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
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.disk.MessageQueue;
import org.zbus.mq.server.auth.Auth;
import org.zbus.mq.server.auth.DefaultAuth;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public abstract class AbstractMQ implements Closeable{	
	protected final String name; 
	protected int mode;
	protected long lastUpdateTime = System.currentTimeMillis();
	
	protected final MessageQueue msgQ; 
	
	protected Auth auth;
	
	protected List<AbstractMQ> slaveMqs = new ArrayList<AbstractMQ>();
	protected AbstractMQ masterMq;
	
	public AbstractMQ(String name, MessageQueue msgQ) {
		this.msgQ = msgQ;
		this.name = name;
		this.auth = new DefaultAuth(msgQ.getAccessToken());
	}
	 
	public String getName() { 
		return name;
	}
 
	public void produce(Message msg, Session sess) throws IOException { 
		msgQ.offer(msg); 
		dispatch();
		
		
		if(slaveMqs.size() == 0){
			return;
		}
		Iterator<AbstractMQ> iter = slaveMqs.iterator();
		while(iter.hasNext()){
			AbstractMQ mq = iter.next();
			try{
				mq.produce(msg, sess);
			} catch(IOException e){
				//ignore slave
				iter.remove();
			}
		} 
	} 
 
	public abstract void consume(Message msg, Session sess) throws IOException;

	abstract void dispatch() throws IOException; 
	
	public abstract void cleanSession(Session sess);
	
	public abstract void cleanSession();
	
	public abstract MqInfo getMqInfo();
	
	public void setAccessToken(String accessToken){ 
		this.msgQ.setAccessToken(accessToken);
		this.auth.setAccessToken(accessToken);
	} 
	
	public String getAccessToken() {
		return msgQ.getAccessToken();
	}

	public void setAuth(Auth auth){
		this.auth = auth;
	}
	
	public boolean auth(String appid, String token){
		if(this.auth == null) return true;
		return this.auth.auth(appid, token);
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}    
	
	public void setMasterMq(AbstractMQ mq){
		mq.addSlaveMq(this);
	}
	
	public String getMasterMqName(){
		if(this.masterMq == null){
			return msgQ.getMasterMq();
		}
		return masterMq.getName();
	}
	
	public AbstractMQ getMasterMq(){
		return this.masterMq;
	}
	
	
	public synchronized void addSlaveMq(AbstractMQ mq){
		if(this.slaveMqs.contains(mq)){
			return;
		}
		mq.masterMq = this;
		mq.msgQ.setMasterMq(this.name);
		
		this.slaveMqs.add(mq);
	}
	
	public synchronized void removeSlaveMq(AbstractMQ mq){
		if(this.slaveMqs.contains(mq)){
			return;
		}
		mq.masterMq = null;
		mq.msgQ.setMasterMq(null);
		this.slaveMqs.remove(mq);
	}

	public String getCreator() {
		return msgQ.getCreator();
	}

	public void setCreator(String creator) {
		this.msgQ.setCreator(creator);
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
	
}

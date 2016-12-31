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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Protocol.ConsumerInfo;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.disk.Index;
import org.zbus.mq.disk.QueueReader;
import org.zbus.mq.disk.QueueWriter;
import org.zbus.net.Session;
import org.zbus.net.http.Message;


public class DiskQueue implements MessageQueue{
	private static final Logger log = LoggerFactory.getLogger(DiskQueue.class); 
	protected Index index;    
	
	protected Map<String, ConsumeGroup> consumeGroups = new ConcurrentHashMap<String, ConsumeGroup>(); 
	protected long lastUpdateTime = System.currentTimeMillis(); 
	
	private final String name; 
	private final QueueWriter writer;
	
	public DiskQueue(Index index) throws IOException{
		this.index = index;
		this.name = index.getName();
		this.writer = new QueueWriter(this.index);
	}
	  
	@Override
	public void produce(Message msg, Session session) throws IOException{   
		writer.write(msg.toBytes()); 
		dispatch();
	}

	@Override
	public void consume(Message message, Session session) throws IOException {
		String readerGroup = message.getConsumeGroup();
		if(readerGroup == null){
			readerGroup = this.name;
		}
		
		ConsumeGroup group = consumeGroups.get(readerGroup);
		if(group == null){
			group = new ConsumeGroup(this.index, readerGroup);  
			consumeGroups.put(readerGroup, group); 
		}   
		
		if(!group.pullSessions.containsKey(session.id())){
			group.pullSessions.put(session.id(), session);
		}
		
		for(PullSession pull : group.pullQ){
			if(pull.getSession() == session){
				pull.setPullMessage(message);
				dispatch(group);
				return; 
			}
		} 
		
		PullSession pull = new PullSession(session, message);
		group.pullQ.offer(pull);  
		dispatch(group);
	}   
	
	void dispatch() throws IOException{  
		Iterator<Entry<String, ConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			ConsumeGroup group = iter.next().getValue();
			dispatch(group);
		} 
	}
	
	void dispatch(ConsumeGroup group) throws IOException{  
		while(group.pullQ.peek() != null && !group.reader.isEOF()){
			Message msg = null;
			PullSession pull = null;
			synchronized (this) { 
				pull = group.pullQ.peek();
				if(pull == null){
					continue;
				}
				if( !pull.getSession().isActive() ){ 
					group.pullQ.poll();
					continue;
				}  
				
				byte[] data = group.reader.read();
				if(data == null){
					break;
				} 
				msg = Message.parse(data);
				if(msg == null){ 
					log.error("data read from queue can not be serialized back to Message type");
					break;
				}
				
				String expire = msg.getHead("expire");
				if(expire != null){
					try{
						long value = Long.valueOf(expire);
						if(value < System.currentTimeMillis()){ 
							log.info("Remove expired message: \n" + msg); //remove message
							continue; //expired message
						}
					} catch(Exception e){
						//expired
					}
				} 
				
				pull = group.pullQ.poll();  
			} 
			
			this.lastUpdateTime = System.currentTimeMillis(); 
			try {  
				Message pullMsg = pull.getPullMessage(); 
				Message writeMsg = Message.copyWithoutBody(msg); 
				
				writeMsg.setOriginId(msg.getId());  //保留原始消息ID
				writeMsg.setId(pullMsg.getId()); //配对订阅消息！
				if(writeMsg.getStatus() == null){
					if(!"/".equals(writeMsg.getUrl())){
						writeMsg.setOriginUrl(writeMsg.getUrl()); 
					}
					writeMsg.setStatus(200); //default to 200
				}
				pull.getSession().write(writeMsg); 
			
			} catch (Exception ex) {   
				log.error(ex.getMessage(), ex);  
			} 
		} 
		
	}
	
	@Override
	public int remaining(String readerGroup) { 
		//TODO
		return 0;
	}
	
	
	@Override
	public String getAccessToken() {
		return index.getExt(1);
	}

	@Override
	public void setAccessToken(String value) { 
		index.setExt(1, value);
	}

	@Override
	public String getCreator() {
		return index.getExt(0);
	}

	@Override
	public void setCreator(String value) { 
		index.setExt(0, value);
	} 
	
	@Override
	public int getFlag() {
		return index.getFlag();
	}
	@Override
	public void setFlag(int value) {
		index.setFlag(value);
	}
	@Override
	public long getLastUpdateTime() { 
		return lastUpdateTime;
	}
	 
	@Override
	public int consumerCount(String readerGroup) {
		if(readerGroup == null){
			readerGroup = this.name;
		}
		
		ConsumeGroup group = consumeGroups.get(readerGroup);
		if(group == null){
			return 0;
		}   
		return group.pullQ.size();
	}
	
	private void cleanSession(ConsumeGroup group, Session sess){
		group.pullSessions.remove(sess.id());
		
		Iterator<PullSession> iter = group.pullQ.iterator();
		while(iter.hasNext()){
			PullSession pull = iter.next();
			if(sess == pull.session){
				iter.remove();
				break;
			}
		}
	}
	
	public void cleanSession(Session sess) {
		Iterator<Entry<String, ConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			ConsumeGroup group = iter.next().getValue();
			cleanSession(group, sess);
		} 
	}
	 
	public void cleanSession() { 
		Iterator<Entry<String, ConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			ConsumeGroup group = iter.next().getValue(); 
			Iterator<PullSession> iterSess = group.pullQ.iterator();
			while(iterSess.hasNext()){
				PullSession pull = iterSess.next();
				if(!pull.session.isActive()){
					group.pullSessions.remove(pull.session.id());
					iterSess.remove();
				}
			}
		}  
	}
	
	@Override
	public MqInfo getMqInfo() {
		MqInfo info = new MqInfo(); 
		info.name = name;
		info.lastUpdateTime = lastUpdateTime;
		info.creator = getCreator();
		info.mode = this.getFlag();
		info.unconsumedMsgCount = remaining(null);//TODO
		info.consumerCount = consumerCount(null);//TODO
		info.consumerInfoList = new ArrayList<ConsumerInfo>();
		ConsumeGroup group = consumeGroups.get(null);
		if(group != null){
			for(PullSession pull : group.pullQ){ 
				info.consumerInfoList.add(pull.getConsumerInfo());
			} 
		}   
		
		return info;
	}
	
	@Override
	public String getName() { 
		return this.name;
	}
	
	private static class ConsumeGroup implements Closeable{ 
		public final QueueReader reader;
		public final String groupName;
		public final BlockingQueue<PullSession> pullQ = new LinkedBlockingQueue<PullSession>();  
		public final Map<String, Session> pullSessions = new ConcurrentHashMap<String, Session>(); 
		
		public ConsumeGroup(Index index, String groupName) throws IOException{ 
			this.groupName = groupName;
			reader = new QueueReader(index, this.groupName);
		}
		
		@Override
		public void close() throws IOException {
			reader.close(); 
		}
	}

}
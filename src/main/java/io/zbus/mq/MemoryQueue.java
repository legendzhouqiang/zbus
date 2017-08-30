package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.server.ReplyKit;
import io.zbus.transport.Session;

public class MemoryQueue implements MessageQueue {
	private static final Logger log = LoggerFactory.getLogger(DiskQueue.class); 
	private Queue<Message> queue = new ConcurrentLinkedQueue<Message>();
	private int capacity = 1000;
	private String topic;
	private int mask = 0; 
	protected long lastUpdateTime = System.currentTimeMillis(); 
	protected long createdTime = System.currentTimeMillis();
	

	protected Map<String, MemoryConsumeGroup> consumeGroups = new ConcurrentSkipListMap<String, MemoryConsumeGroup>(String.CASE_INSENSITIVE_ORDER); 
	
	public MemoryQueue(String topic){
		this.topic = topic;
	}
	
	@Override
	public void produce(Message message) throws IOException {
		queue.offer(message);
		if(queue.size() > capacity){
			queue.poll();
		}
		
		this.lastUpdateTime = System.currentTimeMillis(); 
		dispatch();
	}

	@Override
	public Message consume(String consumeGroup, Integer window) throws IOException {
		return queue.poll();
	}

	@Override
	public void consume(Message message, Session session) throws IOException {
		String consumeGroup = message.getConsumeGroup();
		if(consumeGroup == null){
			consumeGroup = this.topic;
		}  
		
		MemoryConsumeGroup group = consumeGroups.get(consumeGroup);
		if(group == null){
			message.setBody(consumeGroup + " not found");
			ReplyKit.reply404(message, session, "ConsumeGroup(" + consumeGroup + ") Not Found");
			return;
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
	
	
	protected void dispatch() throws IOException{  
		Iterator<Entry<String, MemoryConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			MemoryConsumeGroup group = iter.next().getValue();
			dispatch(group);
		} 
	}
	
	protected void dispatch(MemoryConsumeGroup group) throws IOException{  
		while(group.pullQ.peek() != null && !group.isEnd()){
			Message msg = null;
			PullSession pull = group.pullQ.poll(); 
			if(pull == null) break; 
			if( !pull.getSession().active() ){  
				continue;
			}  
			 
			msg = queue.poll();
			if(msg == null){  
				break;
			}
			msg.setOffset(0L);//FIXME   
			
			this.lastUpdateTime = System.currentTimeMillis(); 
			try {  
				Message pullMsg = pull.getPullMessage(); 
				Message writeMsg = Message.copyWithoutBody(msg); 
				
				writeMsg.setOriginId(msg.getId());  
				writeMsg.setId(pullMsg.getId());
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
	public ConsumeGroupInfo declareGroup(ConsumeGroup ctrl) throws Exception {
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null){
			consumeGroup = this.topic;
		}
		
		MemoryConsumeGroup group = consumeGroups.get(consumeGroup); 
		if(group == null){
			group = new MemoryConsumeGroup(consumeGroup);
			group.filter = ctrl.getFilter();
			group.mask = ctrl.getMask();
			this.consumeGroups.put(consumeGroup, group);
		}
		return group.getConsumeGroupInfo();
	}
	
	@Override
	public ConsumeGroupInfo consumeGroup(String groupName) {
		MemoryConsumeGroup group = consumeGroups.get(groupName); 
		if(group == null) return null;
		return group.getConsumeGroupInfo();
	}

	@Override
	public void removeGroup(String groupName) throws IOException { 
		this.consumeGroups.remove(groupName);
	}

	@Override
	public void removeTopic() throws IOException { 
		
	}

	@Override
	public int consumerCount(String consumeGroup) {   
		if(consumeGroup == null){
			consumeGroup = this.topic;
		}
		
		MemoryConsumeGroup group = consumeGroups.get(consumeGroup);
		if(group == null){
			return 0;
		}   
		return group.pullQ.size();
	}


	public void cleanSession(Session sess) {
		if(sess == null){
			cleanInactiveSessions();
			return;
		}
		
		Iterator<Entry<String, MemoryConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			MemoryConsumeGroup group = iter.next().getValue();
			cleanSession(group, sess);
		} 
	}
	
	private void cleanSession(MemoryConsumeGroup group, Session sess){
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
	 
	private void cleanInactiveSessions() { 
		Iterator<Entry<String, MemoryConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			MemoryConsumeGroup group = iter.next().getValue(); 
			Iterator<PullSession> iterSess = group.pullQ.iterator();
			while(iterSess.hasNext()){
				PullSession pull = iterSess.next();
				if(!pull.session.active()){
					group.pullSessions.remove(pull.session.id());
					iterSess.remove();
				}
			}
		}  
	}
	
	@Override
	public TopicInfo getInfo() {
		TopicInfo info = new TopicInfo(); 
		info.topicName = topic;
		info.createdTime = createdTime;
		info.lastUpdatedTime = lastUpdateTime; 
		info.mask = getMask();
		info.messageDepth = queue.size(); 
		info.consumerCount = 0;
		info.consumeGroupList = new ArrayList<ConsumeGroupInfo>();
		for(MemoryConsumeGroup group : consumeGroups.values()){
			ConsumeGroupInfo groupInfo = group.getConsumeGroupInfo();
			info.consumerCount += groupInfo.consumerCount;
			info.consumeGroupList.add(groupInfo);
		} 
		
		return info;
	}
	

	@Override
	public String getTopic() { 
		return this.topic;
	}
 
	@Override
	public long getUpdateTime() { 
		return this.lastUpdateTime;
	}

	@Override
	public String getCreator() { 
		return "";
	}

	@Override
	public void setCreator(String value) { 

	}

	@Override
	public int getMask() { 
		return mask;
	}

	@Override
	public void setMask(int value) { 
		mask = value;
	}

	private class MemoryConsumeGroup implements Closeable{  
		public final BlockingQueue<PullSession> pullQ = new LinkedBlockingQueue<PullSession>();  
		public final Map<String, Session> pullSessions = new ConcurrentHashMap<String, Session>();  
		
		private String groupName;
		private String filter;
		private Integer mask;
		private long createdTime = System.currentTimeMillis();
		private long updatedTime = System.currentTimeMillis();
		
		public MemoryConsumeGroup(String groupName){
			this.groupName = groupName;
		}
		
		@Override
		public void close() throws IOException { 
			
		} 
		
		public boolean isEnd(){
			return queue.size() == 0;
		}
		 
		public ConsumeGroupInfo getConsumeGroupInfo(){
			ConsumeGroupInfo info = new ConsumeGroupInfo(); 
			info.topicName = topic;
			info.filter = filter;
			info.creator = null;
			info.mask = mask == null? 0 : mask;
			info.createdTime = createdTime;
			info.lastUpdatedTime = updatedTime;
			info.consumerCount = pullSessions.size();
			info.messageCount = queue.size();
			info.groupName = groupName;
			info.consumerList = new ArrayList<String>();
			for(Session session : pullSessions.values()){
				info.consumerList.add(session.remoteAddress());
			}
			return info;
		}
	}
}

package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;
import io.zbus.net.Session;


public class DiskQueue implements MessageQueue{
	private static final Logger log = LoggerFactory.getLogger(DiskQueue.class); 
	protected final Index index;    
	
	protected Map<String, DiskConsumeGroup> consumeGroups = new ConcurrentSkipListMap<String, DiskConsumeGroup>(String.CASE_INSENSITIVE_ORDER); 
	protected long lastUpdateTime = System.currentTimeMillis(); 
	
	private final String name; 
	private final QueueWriter writer;
	
	public DiskQueue(Index index) throws IOException{
		this.index = index;
		this.name = index.getName();
		this.writer = new QueueWriter(this.index);
		loadConsumeGroups();
	}
	
	public DiskQueue(File dir) throws IOException { 
		this(new Index(dir));
	}
	
	private void loadConsumeGroups() throws IOException{ 
        File[] readerFiles = index.getReaderDir().listFiles(new FileFilter() { 
			@Override
			public boolean accept(File pathname) {
				return Index.isReaderFile(pathname);
			}
		});
        if (readerFiles != null && readerFiles.length> 0) {
            for (File readerFile : readerFiles) {  
            	String groupName = readerFile.getName();
            	groupName = groupName.substring(0, groupName.lastIndexOf('.'));
            	
            	DiskConsumeGroup group = new DiskConsumeGroup(this.index, groupName);
            	consumeGroups.put(groupName, group);
            }
        } 
	}
	  
	public ConsumeGroupInfo declareGroup(ConsumeGroup ctrl) throws Exception{
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null){
			consumeGroup = this.name;
		}
		DiskConsumeGroup group = consumeGroups.get(consumeGroup); 
		if(group == null){
			QueueReader copyReader = null;
			//1) copy reader from base group 
			if(ctrl.getStartCopy() != null){
				DiskConsumeGroup copyGroup = consumeGroups.get(ctrl.getStartCopy());
				if(copyGroup != null){
					copyReader = copyGroup.reader; 
				}
			}
			//2) default to copy latest one
			if(copyReader == null){ 
				copyReader = findLatestReader(); 
			}  
			
			if(copyReader != null){ 
				group = new DiskConsumeGroup(copyReader, consumeGroup);  
			} else {  
				//3) consume from the very beginning
				group = new DiskConsumeGroup(this.index, consumeGroup);  
			}   
			String creator = ctrl.getCreator();
			if(creator != null){
				group.reader.setCreator(ctrl.getCreator());
			}
			consumeGroups.put(consumeGroup, group); 
		} 
		group.reader.setFilter(ctrl.getFilter());
		Integer groupFlag = ctrl.getMask();
		if(groupFlag != null){
			group.reader.setMask(groupFlag);
		}
		
		if(ctrl.getStartOffset() != null){
			boolean seekOk = group.reader.seek(ctrl.getStartOffset(), ctrl.getStartMsgId());
			if(!seekOk){
				String errorMsg = String.format("seek by offset unsuccessfull: (offset=%d, msgid=%s)", ctrl.getStartOffset(), ctrl.getStartMsgId());
				throw new IllegalArgumentException(errorMsg);
			}
		} else { 
			if(ctrl.getStartTime() != null){
				boolean seekOk = group.reader.seek(ctrl.getStartTime());
				if(!seekOk){
					String errorMsg = String.format("seek by time unsuccessfull: (time=%d)", ctrl.getStartTime());
					throw new IllegalArgumentException(errorMsg);
				}
			}
		}
		return group.getConsumeGroupInfo();
	}   
	
	@Override
	public void removeGroup(String groupName) throws IOException {
		DiskConsumeGroup group = consumeGroups.remove(groupName); 
		if(group == null){
			throw new FileNotFoundException("ConsumeGroup("+groupName+") Not Found"); 
		}
		group.delete();
	}
	
	@Override
	public void removeTopic() throws IOException { 
		for(DiskConsumeGroup group : consumeGroups.values()){
			group.delete();
		}
		writer.close();
		index.delete(); 
	}
	
	@Override
	public void produce(Message msg, Session session) throws IOException{ 
		DiskMessage data = new DiskMessage();
		data.id = msg.getId();
		data.tag = msg.getTag(); 

		data.body = msg.toBytes(); 
		writer.write(data); 
		this.lastUpdateTime = System.currentTimeMillis(); 
		dispatch();
	}

	@Override
	public void consume(Message message, Session session) throws IOException {
		String consumeGroup = message.getConsumeGroup();
		if(consumeGroup == null){
			consumeGroup = this.name;
		} 
		DiskConsumeGroup group = consumeGroups.get(consumeGroup);
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
	
	private QueueReader findLatestReader(){ 
		List<DiskConsumeGroup> readerList = new ArrayList<DiskConsumeGroup>(consumeGroups.values());
		if(readerList.isEmpty()) return null; 
		Collections.sort(readerList, new Comparator<DiskConsumeGroup>() { 
			@Override
			public int compare(DiskConsumeGroup o1, DiskConsumeGroup o2) {
				return - o1.reader.compareTo(o2.reader);
			}
		}); 
		return readerList.get(0).reader;
	}
	
	protected void dispatch() throws IOException{  
		Iterator<Entry<String, DiskConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			DiskConsumeGroup group = iter.next().getValue();
			dispatch(group);
		} 
	}
	
	protected void dispatch(DiskConsumeGroup group) throws IOException{  
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
				
				DiskMessage data = group.reader.read();
				if(data == null){
					break;
				} 
				msg = Message.parse(data.body);
				if(msg == null){ 
					log.error("data read from queue can not be serialized back to Message type");
					break;
				}
				msg.setOffset(data.offset); 
				
				pull = group.pullQ.poll();  
			} 
			
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
	 
	public long remaining(String consumeGroup) { 
		if(consumeGroup == null){
			consumeGroup = this.name;
		} 
		DiskConsumeGroup group = consumeGroups.get(consumeGroup);
		if(group == null){
			return 0;
			//throw new IllegalArgumentException(consumeGroup + " not found");
		}   
		 
		return index.getMessageCount() - group.reader.getMessageNumber()-1;
	} 
	
	@Override
	public String getCreator() {
		return index.getCreator();
	}

	@Override
	public void setCreator(String value) { 
		index.setCreator(value);
	} 
	
	@Override
	public int getMask() {
		return index.getMask();
	}
	@Override
	public void setMask(int value) {
		index.setMask(value);
	}
	@Override
	public long lastUpdateTime() { 
		return lastUpdateTime;
	}
	 
	@Override
	public int consumerCount(String consumeGroup) {
		if(consumeGroup == null){
			consumeGroup = this.name;
		}
		
		DiskConsumeGroup group = consumeGroups.get(consumeGroup);
		if(group == null){
			return 0;
		}   
		return group.pullQ.size();
	}
	
	private void cleanSession(DiskConsumeGroup group, Session sess){
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
		Iterator<Entry<String, DiskConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			DiskConsumeGroup group = iter.next().getValue();
			cleanSession(group, sess);
		} 
	}
	 
	public void cleanInactiveSessions() { 
		Iterator<Entry<String, DiskConsumeGroup>> iter = consumeGroups.entrySet().iterator();
		while(iter.hasNext()){
			DiskConsumeGroup group = iter.next().getValue(); 
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
	public TopicInfo topicInfo() {
		TopicInfo info = new TopicInfo(); 
		info.topicName = name;
		info.createdTime = index.getCreatedTime();
		info.lastUpdatedTime = lastUpdateTime;
		info.creator = getCreator();
		info.mask = getMask();
		info.messageDepth = index.getMessageCount();  
		info.consumerCount = 0;
		info.consumeGroupList = new ArrayList<ConsumeGroupInfo>();
		for(DiskConsumeGroup group : consumeGroups.values()){
			ConsumeGroupInfo groupInfo = group.getConsumeGroupInfo();
			info.consumerCount += groupInfo.consumerCount;
			info.consumeGroupList.add(groupInfo);
		} 
		
		return info;
	}
	
	@Override
	public String name() { 
		return this.name;
	} 
	
	private static class DiskConsumeGroup implements Closeable{ 
		public final QueueReader reader;
		public final String groupName;
		public final BlockingQueue<PullSession> pullQ = new LinkedBlockingQueue<PullSession>();  
		public final Map<String, Session> pullSessions = new ConcurrentHashMap<String, Session>(); 
		
		public DiskConsumeGroup(Index index, String groupName) throws IOException{ 
			this.groupName = groupName;
			reader = new QueueReader(index, this.groupName);
		}
		
		public DiskConsumeGroup(QueueReader reader, String groupName) throws IOException{ 
			this.groupName = groupName;
			this.reader = new QueueReader(reader, groupName);
		}
		
		@Override
		public void close() throws IOException {
			reader.close(); 
		} 
		
		public void delete() throws IOException{
			reader.delete();
		}
		
		public ConsumeGroupInfo getConsumeGroupInfo(){
			ConsumeGroupInfo info = new ConsumeGroupInfo(); 
			info.topicName = reader.getIndexName();
			info.filter = reader.getFilter();
			info.creator = reader.getCreator();
			info.mask = reader.getMask();
			info.createdTime = reader.getCreatedTime();
			info.lastUpdatedTime = reader.getUpdatedTime();
			info.consumerCount = pullSessions.size();
			info.messageCount = reader.getMessageCount();
			info.groupName = groupName;
			info.consumerList = new ArrayList<String>();
			for(Session session : pullSessions.values()){
				info.consumerList.add(session.getRemoteAddress());
			}
			return info;
		}
	} 
}
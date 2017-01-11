package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
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
import java.util.concurrent.LinkedBlockingQueue;

import io.zbus.kit.log.Logger;
import io.zbus.kit.log.LoggerFactory;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Protocol.ConsumerInfo;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;
import io.zbus.net.Session;
import io.zbus.net.http.Message;


public class DiskQueue implements MessageQueue{
	private static final Logger log = LoggerFactory.getLogger(DiskQueue.class); 
	protected final Index index;    
	
	protected Map<String, DiskConsumeGroup> consumeGroups = new ConcurrentHashMap<String, DiskConsumeGroup>(); 
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
	  
	public void declareConsumeGroup(ConsumeGroup ctrl) throws Exception{
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null){
			consumeGroup = this.name;
		}
		DiskConsumeGroup group = consumeGroups.get(consumeGroup); 
		if(group == null){
			QueueReader copyReader = null;
			//1) copy reader from base group 
			if(ctrl.getBaseGroupName() != null){
				DiskConsumeGroup copyGroup = consumeGroups.get(ctrl.getBaseGroupName());
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
			consumeGroups.put(consumeGroup, group); 
		}
		
		group.reader.setFilterTag(ctrl.getFilterTag());
		
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
	}   
	
	@Override
	public void produce(Message msg, Session session) throws IOException{ 
		DiskMessage data = new DiskMessage();
		data.id = msg.getId();
		data.tag = msg.getTag(); 

		data.body = msg.toBytes(); 
		writer.write(data); 
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
			ReplyKit.reply404(message, session);
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
	
	@Override
	public MessageQueue childMessageQueue() {
		// TODO Auto-generated method stub
		return null;
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
				
				Long expire = msg.getExpire();
				if(expire != null){
					try{ 
						if(expire < System.currentTimeMillis()){ 
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
	public long remaining(String consumeGroup) { 
		if(consumeGroup == null){
			consumeGroup = this.name;
		} 
		DiskConsumeGroup group = consumeGroups.get(consumeGroup);
		if(group == null){
			throw new IllegalArgumentException(consumeGroup + " not found");
		}   
		 
		return index.getMessageCount() - group.reader.getMessageCount(); //reader.messageCount is the message to read
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
	 
	public void cleanSession() { 
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
	public MqInfo getMqInfo() {
		MqInfo info = new MqInfo(); 
		info.name = name;
		info.lastUpdateTime = lastUpdateTime;
		info.creator = getCreator();
		info.mode = this.getFlag();
		info.unconsumedMsgCount = remaining(name);//TODO
		info.consumerCount = consumerCount(name);//TODO
		info.consumerInfoList = new ArrayList<ConsumerInfo>();
		DiskConsumeGroup group = consumeGroups.get(name);
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
	} 
}
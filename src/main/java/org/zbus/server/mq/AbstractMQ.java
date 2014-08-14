package org.zbus.server.mq;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.Message;
import org.zbus.common.BrokerMqInfo;
import org.zbus.common.ConsumerInfo;
import org.znet.Session;

public abstract class AbstractMQ {  
	private static final Logger log = LoggerFactory.getLogger(AbstractMQ.class);
	
	protected final String name; 
	protected String creator;
	protected long createdTime = System.currentTimeMillis();
	protected String accessToken = "";
	protected final long mode;
	protected final BlockingQueue<Message> msgQ = new LinkedBlockingQueue<Message>();
	protected final ExecutorService executor;
	
	public AbstractMQ(String name, ExecutorService executor, int mode){
		this.name = name; 
		this.executor = executor; 
		this.mode = mode;
	}  
	 
	public void produce(Message msg, Session sess) throws IOException{
		String msgId = msg.getMsgId(); 
		if(msg.isAck()){
			ReplyHelper.reply200(msgId, sess);
		} 
		
    	msgQ.offer(msg);  
    	this.dispatch();
	}
 
	public abstract void consume(Message msg, Session sess) throws IOException;
	
	abstract void doDispatch() throws IOException;
	public abstract void cleanSession();
	void dispatch(){
		executor.submit(new Runnable() {
			@Override
			public void run() { 
				try {
					AbstractMQ.this.doDispatch();
				} catch (IOException e) { 
					log.error(e.getMessage(), e);
				}
			}
		});
	}
	
	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getName() {
		return name;
	}

	public String getCreator() {
		return creator;
	} 
	
	
	public void setCreator(String creator) {
		this.creator = creator;
	}
	

	public long getMode() {
		return mode;
	}

	public BrokerMqInfo getMqInfo(){
		BrokerMqInfo info = new BrokerMqInfo();
		info.setName(name);
		info.setCreator(creator);
		info.setCreatedTime(createdTime);
		info.setUnconsumedMsgCount(msgQ.size()); 
		info.setConsumerInfoList(getConsumerInfoList());
		info.setMode(this.mode);
		return info;
	}
	
	public abstract List<ConsumerInfo> getConsumerInfoList();

	@Override
	public String toString() {
		return "MQ [name=" + name + ", creator=" + creator + ", createdTime="
				+ createdTime + "]";
	}
	
	
}

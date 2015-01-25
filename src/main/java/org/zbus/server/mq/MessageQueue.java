package org.zbus.server.mq;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.zbus.common.MqInfo;
import org.zbus.common.ConsumerInfo;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.nio.Session;

public abstract class MessageQueue implements Serializable{  
	private static final long serialVersionUID = 3408125142128217794L;

	private static final Logger log = LoggerFactory.getLogger(MessageQueue.class);
	
	protected final String broker; 
	protected final String name; 
	protected String creator;
	protected long createdTime = System.currentTimeMillis();
	protected String accessToken = "";
	protected final int mode;
	
	protected transient ExecutorService executor;
	
	public MessageQueue(String broker, String name, ExecutorService executor, int mode){
		this.broker = broker;
		this.name = name; 
		this.executor = executor; 
		this.mode = mode;
	}  
	 
	public abstract void produce(Message msg, Session sess) throws IOException;
	public abstract void consume(Message msg, Session sess) throws IOException;
	
	abstract void doDispatch() throws IOException;
	public abstract void cleanSession();
	void dispatch(){
		executor.submit(new Runnable() {
			@Override
			public void run() { 
				try {
					MessageQueue.this.doDispatch();
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
	

	public int getMode() {
		return mode;
	} 

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public MqInfo getMqInfo(){
		MqInfo info = new MqInfo(); 
		info.setBroker(broker);
		info.setName(name);
		info.setCreator(creator);
		info.setCreatedTime(createdTime);
		info.setUnconsumedMsgCount(this.getMessageQueueSize()); 
		info.setConsumerInfoList(getConsumerInfoList());
		info.setMode(this.mode);
		return info; 
	}
	
	public abstract int getMessageQueueSize();
	public abstract List<ConsumerInfo> getConsumerInfoList();
	public abstract void restoreFromDump(ExecutorService executor);

	@Override
	public String toString() {
		return "MQ [name=" + name + ", creator=" + creator + ", createdTime="
				+ createdTime + "]";
	}
	
	
}

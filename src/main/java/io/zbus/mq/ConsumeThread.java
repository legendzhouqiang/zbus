package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.ThreadKit.ManualResetEvent;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;

public class ConsumeThread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(ConsumeThread.class);  
	protected final MqClient client;
	
	protected String topic;  
	protected Integer topicMask;
	protected String token;
	protected ConsumeGroup consumeGroup;
	protected int consumeTimeout = 10000;
	protected Integer consumeWindow;
	
	private String consumeGroupName; //internally upated
	 
	
	protected ExecutorService consumeRunner;
	protected MessageHandler messageHandler;
	
	protected ManualResetEvent active = new ManualResetEvent(true); 
	
	protected RunningThread consumeThread;
	 
	public ConsumeThread(MqClient client, String topic, ConsumeGroup group, Integer topicMask){
		this.client = client;
		this.topic = topic;
		this.consumeGroup = group;
		this.topicMask = topicMask;
	}
	
	public ConsumeThread(MqClient client, String topic){
		this(client, topic, null, null);
	}
	
	public ConsumeThread(MqClient client){
		this(client, null);
	}
	 
	public synchronized void start() {
		start(false);
	}
	
	public synchronized void start(boolean pauseOnStart) {
		if(this.topic == null){
			throw new IllegalStateException("Missing topic");
		}
		if(this.messageHandler == null){
			throw new IllegalStateException("Missing consumeHandler");
		}
		if(pauseOnStart){
			active.reset();
		}
		
		if(this.consumeGroup == null){
			this.consumeGroup = new ConsumeGroup();
			consumeGroup.setGroupName(this.topic);
		}  
		this.client.setToken(token);
		this.client.setInvokeTimeout(consumeTimeout);
		try { 
			ConsumeGroupInfo info = this.client.declareGroup(topic, consumeGroup, topicMask);
			consumeGroupName = info.groupName; //update groupName
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		} catch (InterruptedException e) { 
			log.error(e.getMessage(), e);
		}
		
		
		consumeThread = new RunningThread();
		consumeThread.start();
	}
	
	public void pause(){
		try {
			client.unconsume(topic, consumeGroupName); //stop consuming in serverside
			consumeThread.running = false;
			consumeThread.interrupt();
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}  
		
		active.reset();
	}
	
	public void resume(){  
		active.set();
		if(consumeThread == null || !consumeThread.running) {
			consumeThread = new RunningThread();
			consumeThread.start();
		}
	}
	
	public Message take() throws IOException, InterruptedException {  
		Message res = null;
		try {  
			res = client.consume(topic, this.consumeGroupName, this.getConsumeWindow()); 
			if (res == null) return res; 
			Integer status = res.getStatus();
			if (status == 404) { 
				ConsumeGroupInfo info = client.declareGroup(topic, consumeGroup, topicMask);
				consumeGroupName = info.groupName; //update groupName
				return take();
			}
			
			if (status == 200) { 
				String originUrl = res.getOriginUrl();
				if(originUrl != null){ 
					res.removeHeader(Protocol.ORIGIN_URL);
					res.setUrl(originUrl);   
					res.setStatus(null);
					String method = res.getOriginMethod();
					if(method != null){
						res.setMethod(method);
						res.removeHeader(Protocol.ORIGIN_METHOD);
					}
					return res;
				}
				
				Integer originStatus = res.getOriginStatus();
				if(originStatus != null){ 
					res.removeHeader(Protocol.ORIGIN_STATUS);  
					res.setStatus(originStatus);
					return res;
				}
				
				res.setStatus(null);//default to request type
				return res;
			}
			
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		}   
	} 
	
	
	class RunningThread extends Thread {
		volatile boolean running = true; 
		
		public void run() { 
			while (running) {
				try { 
					final Message msg;
					try {
						while(running){
							active.await(1000, TimeUnit.MILLISECONDS); 
							if(active.isSignalled()) break;
						}
						if(!running) break; 
						msg = take();
						if(msg == null) continue;
						if(!running) break;
						
						if(messageHandler == null){
							throw new IllegalStateException("Missing ConsumeHandler");
						}
						
						if(consumeRunner == null){
							try{
								messageHandler.handle(msg, client);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
						} else {
							consumeRunner.submit(new Runnable() { 
								@Override
								public void run() {
									try{
										messageHandler.handle(msg, client);
									} catch (Exception e) {
										log.error(e.getMessage(), e);
									}
								}
							});
						}
						
					} catch (InterruptedException e) {
						client.close(); 
						break;
					}  
					
				} catch (IOException e) {  
					log.error(e.getMessage(), e);  
				}
			}
		}
	}
	  

	@Override
	public void close() throws IOException {
		consumeThread.interrupt();  
	} 

	public ExecutorService getConsumeRunner() {
		return consumeRunner;
	}

	public void setConsumeRunner(ExecutorService consumeRunner) {
		this.consumeRunner = consumeRunner;
	}

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	} 
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}  
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(ConsumeGroup consumeGroup) {
		this.consumeGroup = consumeGroup;
	}

	public int getConsumeTimeout() {
		return consumeTimeout;
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public Integer getConsumeWindow() {
		return consumeWindow;
	}

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	}

	public MqClient getClient() {
		return client;
	}

	public Integer getTopicMask() {
		return topicMask;
	}

	public void setTopicMask(Integer topicMask) {
		this.topicMask = topicMask;
	}  
	
}
package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory; 

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class); 
	protected final Broker broker; 
	
	private ConsumeGroup consumeGroup;
	private Integer consumeWindow; 
	private int consumeTimeout = 120000; // 2 minutes 
	
	private Object invokerInitLock = new Object();

	public Consumer(Broker broker, String topic) {
		this.broker = broker;
		this.topic = topic;
	} 
	
	public Consumer(MqConfig config) {
		this.broker = config.getBroker();
		this.topic = config.getTopic();
		this.flag = config.getFlag();
		this.appid = config.getAppid();
		this.token = config.getToken();
		this.invokeTimeout = config.getInvokeTimeout();
		
		this.consumeGroup = config.getConsumeGroup();
		this.consumeWindow = config.getConsumeWindow();
		this.consumeTimeout = config.getConsumeTimeout();
	} 

	public Message take(int timeout) throws IOException, InterruptedException { 
		Message req = new Message();
		req.setCommand(Protocol.Consume);
		req.setConsumeWindow(consumeWindow);
		fillCommonHeaders(req);  
		if(consumeGroup != null){ //consumeGroup
			req.setConsumeGroup(consumeGroup.getGroupName());
		}

		Message res = null;
		try {  
			res = invokeSync(req, timeout);
			if (res == null)
				return res;
			res.setId(res.getOriginId());
			res.removeHead(Protocol.ORIGIN_ID);
			if ("200".equals(res.getStatus())){
				String originUrl = res.getOriginUrl();
				if(originUrl == null){
					originUrl = "/";
				} else {
					res.removeHead(Protocol.ORIGIN_URL);
				}
				res.setUrl(originUrl);
				return res;
			}

			if ("404".equals(res.getStatus())) {
				if (!this.declareTopic()) {
					throw new MqException(res.getBodyString());
				}
				return take(timeout);
			}
			
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			try {
				broker.releaseInvoker(messageInvoker); 
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			} finally{
				synchronized (invokerInitLock) {
					this.messageInvoker = null;
				}
			}
		}
		return res;
	}

	public Message take() throws InterruptedException, IOException {
		while (true) {
			Message message = take(consumeTimeout);
			if (message == null)
				continue;
			return message; 
		}
	}  
	
	protected Message buildDeclareTopicMessage(){
		Message req = super.buildDeclareTopicMessage();  
    	if(this.consumeGroup != null){
	    	req.setConsumeGroup(consumeGroup.getGroupName());
	    	req.setConsumeBaseGroup(consumeGroup.getBaseGroupName());
	    	req.setConsumeStartOffset(consumeGroup.getStartOffset());
	    	req.setConsumeStartMsgId(consumeGroup.getStartMsgId());
	    	req.setConsumeStartTime(consumeGroup.getStartTime());
	    	req.setConsumeFilterTag(consumeGroup.getFilterTag());
		}
    	return req;
	} 
	
	private void initMessageInvokerIfNeeded() throws IOException{
		if (this.messageInvoker == null) {
			synchronized (invokerInitLock) { 
				if (this.messageInvoker == null) {
					this.messageInvoker = broker.selectForConsumer(this.topic);
				}
			} 
		}  
	}

	@Override
	protected Message invokeSync(Message req, int timeout) throws IOException, InterruptedException { 
		initMessageInvokerIfNeeded();
		return messageInvoker.invokeSync(req, timeout);  
	}
	
	@Override
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		initMessageInvokerIfNeeded();  
		messageInvoker.invokeAsync(req, callback); 
	} 
	 
	public void reply(Message msg) throws IOException {
		msg.setCommand(Protocol.Route);
		msg.setAck(false); 
		String status = msg.getStatus();
		if(status != null){
			msg.setOriginStatus(status); 
			msg.setStatus(null); //make it as request 
		} 
		invokeAsync(msg, null); 
	} 
	
	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(ConsumeGroup consumeGroup) {
		this.consumeGroup = consumeGroup;
	} 
	
	public void setConsumeGroup(String consumeGroup) {
		this.consumeGroup = new ConsumeGroup(consumeGroup);
	} 
	
	public Integer getConsumeWindow() {
		return consumeWindow;
	}

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	}  

	public int getConsumeTimeout() {
		return consumeTimeout;
	}

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public void close() throws IOException { 
		try {
			if (this.messageInvoker != null) {
				this.broker.releaseInvoker(this.messageInvoker);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} 
	} 
}

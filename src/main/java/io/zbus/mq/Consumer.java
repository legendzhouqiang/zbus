package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory; 

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class); 
	private MessageInvoker messageInvoker;   
	
	private int consumeTimeout = 120000; // 2 minutes   
	private ConsumeGroup consumeGroup;
	private Integer consumeWindow;
	
	private volatile Thread consumerThread = null;
	private volatile ConsumerHandler consumerHandler; 
	private int consumeThreadPoolSize = 64;
	private int maxInFlightMessage = 64;
	private boolean consumeInPool = false;
	private ThreadPoolExecutor consumeExecutor;  
	private boolean ownConsumeExecutor = false;

	public Consumer(Broker broker, String topic) {
		super(broker, topic);
	} 
	
	public Consumer(MqConfig config) {
		super(config);   
		this.consumeGroup = config.getConsumeGroup();
		this.consumeWindow = config.getConsumeWindow();
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
			synchronized (this) {
				if (this.messageInvoker == null) {
					this.messageInvoker = broker.selectInvoker(this.topic);
				}
				res = messageInvoker.invokeSync(req, timeout);
			} 
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
				synchronized (this) {
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

	@Override
	protected Message invokeSync(Message req) throws IOException, InterruptedException { 
		synchronized (this) {
			if (this.messageInvoker == null) {
				this.messageInvoker = broker.selectInvoker(this.topic);
			}
			return messageInvoker.invokeSync(req, invokeTimeout);
		} 
	}
	
	@Override
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		synchronized (this) {
			if (this.messageInvoker == null) {
				this.messageInvoker = broker.selectInvoker(this.topic);
			}
			messageInvoker.invokeAsync(req, callback);
		} 
	} 
	 
	public void routeMessage(Message msg) throws IOException {
		msg.setCommand(Protocol.Route);
		msg.setAck(false); 
		String status = msg.getStatus();
		if(status != null){
			msg.setOriginStatus(status); 
			msg.setStatus(null); //make it as request 
		} 
		messageInvoker.invokeAsync(msg, null); 
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

    //-------------------------------------------------------------------------  
	private void initConsumerHandlerPoolIfNeeded(){
		if(consumeInPool && consumeExecutor == null){
			consumeExecutor = new ThreadPoolExecutor(consumeThreadPoolSize, 
					consumeThreadPoolSize, 120, TimeUnit.SECONDS, 
					new LinkedBlockingQueue<Runnable>(maxInFlightMessage),
					new ThreadPoolExecutor.CallerRunsPolicy());
			ownConsumeExecutor = true;
		}
	}
	private final Runnable consumerTask = new Runnable() {
		@Override
		public void run() {
			initConsumerHandlerPoolIfNeeded(); 
			while (true) {
				try {
					final Message msg;
					try {
						msg = take();
					} catch (InterruptedException e) {
						Consumer.this.close();
						break;
					} catch (MqException e) { 
						throw e; 
					} 
					if (consumerHandler == null) {
						log.warn("Missing consumerHandler, call onMessage first");
						continue;
					}
					
					if (consumeInPool && consumeExecutor != null) { 
						consumeExecutor.submit(new Runnable() {
							@Override
							public void run() {
								try {
									consumerHandler.handle(msg, Consumer.this);
								} catch (IOException e) {
									log.error(e.getMessage(), e);
								}
							}
						});
					} else {
						try {
							consumerHandler.handle(msg, Consumer.this);
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
					
				} catch (IOException e) {  
					log.error(e.getMessage(), e); 
				}
			}
		}
	};

	public void onMessage(final ConsumerHandler handler) throws IOException { 
		this.consumerHandler = handler; 
	} 

	public void close() throws IOException {
		stop(); 
	}
	
	public void stop() {
		if (consumerThread != null) {
			consumerThread.interrupt();
			consumerThread = null;
		}
		if(ownConsumeExecutor && consumeExecutor != null){
			consumeExecutor.shutdown();
			consumeExecutor = null;
		}
		try {
			if (this.messageInvoker != null) {
				this.broker.releaseInvoker(this.messageInvoker);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} 
	}
	
	public synchronized void start(ConsumerHandler handler) throws IOException{
		onMessage(handler);
		start();
	}

	public synchronized void start() throws IOException { 
		if (consumerThread == null) {
			consumerThread = new Thread(consumerTask);
			consumerThread.setName("ConsumerThread");
		}

		if (consumerThread.isAlive())
			return;
		consumerThread.start();
	} 

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 
	
	public int getConsumeThreadPoolSize() {
		return consumeThreadPoolSize;
	}

	public void setConsumeThreadPoolSize(int consumeThreadPoolSize) {
		this.consumeThreadPoolSize = consumeThreadPoolSize; 
	} 

	public int getMaxInFlightMessage() {
		return maxInFlightMessage;
	}

	public void setMaxInFlightMessage(int maxInFlightMessage) {
		this.maxInFlightMessage = maxInFlightMessage;
	}
 
	public boolean isConsumeInPool() {
		return consumeInPool;
	}

	public void setConsumeInPool(boolean consumeInPool) {
		this.consumeInPool = consumeInPool;
	}    

	public ThreadPoolExecutor getConsumeExecutor() {
		return consumeExecutor;
	}

	public void setConsumeExecutor(ThreadPoolExecutor consumeExecutor) {
		if(this.consumeExecutor != null && ownConsumeExecutor){
			this.consumeExecutor.shutdown();
		}
		this.consumeExecutor = consumeExecutor;
	} 
}

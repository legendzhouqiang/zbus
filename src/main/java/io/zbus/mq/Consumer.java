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
	private MessageInvoker client;   
	
	private int consumeTimeout = 120000; // 2 minutes   
	private ConsumeGroup consumeGroup;
	private Integer consumeWindow;

	public Consumer(Broker broker, String mq) {
		super(broker, mq);
	} 
	
	public Consumer(MqConfig config) {
		super(config);   
		this.consumeGroup = config.getConsumeGroup();
		this.consumeWindow = config.getConsumeWindow();
	} 
 
	
	protected Message buildDeclareMQMessage(){
		Message req = super.buildDeclareMQMessage();  
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

	public Message take(int timeout) throws IOException, InterruptedException { 
		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setConsumeWindow(consumeWindow);
		fillCommonHeaders(req);  
		if(consumeGroup != null){ //consumeGroup
			req.setConsumeGroup(consumeGroup.getGroupName());
		}

		Message res = null;
		try {  
			synchronized (this) {
				if (this.client == null) {
					this.client = broker.selectInvoker(this.mq);
				}
				res = client.invokeSync(req, timeout);
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
				if (!this.declareQueue()) {
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
				broker.releaseInvoker(client); 
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			} finally{
				synchronized (this) {
					this.client = null;
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

	@Override
	protected Message invokeSync(Message req) throws IOException, InterruptedException { 
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.selectInvoker(this.mq);
			}
			return client.invokeSync(req, 10000);
		} 
	}
	
	@Override
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.selectInvoker(this.mq);
			}
			client.invokeAsync(req, callback);
		} 
	}

	 
	public void routeMessage(Message msg) throws IOException {
		msg.setCmd(Protocol.Route);
		msg.setAck(false); 
		String status = msg.getStatus();
		if(status != null){
			msg.setOriginStatus(status); 
			msg.setStatus(null); //make it as request 
		} 
		client.invokeAsync(msg, null); 
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


	//The followings are all related to start consumer cycle in another thread
	private volatile Thread consumerThread = null;
	private volatile ConsumerHandler consumerHandler; 
	private int consumerHandlerPoolSize = 64;
	private int inFlightMessageCount = 64;
	private boolean consumerHandlerRunInPool = false;
	private ThreadPoolExecutor consumerHandlerExecutor;  
	private boolean ownConsumerHandlerExecutor = false;
	
	private void initConsumerHandlerPoolIfNeeded(){
		if(consumerHandlerRunInPool && consumerHandlerExecutor == null){
			consumerHandlerExecutor = new ThreadPoolExecutor(consumerHandlerPoolSize, 
					consumerHandlerPoolSize, 120, TimeUnit.SECONDS, 
					new LinkedBlockingQueue<Runnable>(inFlightMessageCount),
					new ThreadPoolExecutor.CallerRunsPolicy());
			ownConsumerHandlerExecutor = true;
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
					
					if (consumerHandlerRunInPool && consumerHandlerExecutor != null) { 
						consumerHandlerExecutor.submit(new Runnable() {
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
		if(ownConsumerHandlerExecutor && consumerHandlerExecutor != null){
			consumerHandlerExecutor.shutdown();
			consumerHandlerExecutor = null;
		}
		try {
			if (this.client != null) {
				this.broker.releaseInvoker(this.client);
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
	
	public int getConsumerHandlerPoolSize() {
		return consumerHandlerPoolSize;
	}

	public void setConsumerHandlerPoolSize(int consumerHandlerPoolSize) {
		this.consumerHandlerPoolSize = consumerHandlerPoolSize; 
	} 

	public int getInFlightMessageCount() {
		return inFlightMessageCount;
	}

	public void setInFlightMessageCount(int inFlightMessageCount) {
		this.inFlightMessageCount = inFlightMessageCount;
	}
 
	public boolean isConsumeHandlerRunInPool() {
		return consumerHandlerRunInPool;
	}

	public void setConsumerHandlerRunInPool(boolean consumerHandlerRunInPool) {
		this.consumerHandlerRunInPool = consumerHandlerRunInPool;
	}    

	public ThreadPoolExecutor getConsumeExecutor() {
		return consumerHandlerExecutor;
	}

	public void setConsumerHandlerExecutor(ThreadPoolExecutor consumerHandlerExecutor) {
		if(this.consumerHandlerExecutor != null && ownConsumerHandlerExecutor){
			this.consumerHandlerExecutor.shutdown();
		}
		this.consumerHandlerExecutor = consumerHandlerExecutor;
	} 
}

package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.Broker.ServerNotifyListener;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class ConsumerService implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConsumerService.class); 
	private ConsumerServiceConfig config; 
	private Map<String, ConsumerThreadGroup> consumerThreadGroupMap = new ConcurrentHashMap<String, ConsumerThreadGroup>(); 
	private ThreadPoolExecutor consumerExecutor;  
	private ConsumerHandler consumerHandler; 
	private boolean isStarted = false; 
	 
	public ConsumerService(ConsumerServiceConfig config){ 
		init(config);
	}
	
	public ConsumerService(Broker broker, String topic){
		ConsumerServiceConfig config = new ConsumerServiceConfig();
		config.setBroker(broker);
		config.setTopic(topic);
		config.setConsumerCount(1); //default to single Consumer
		
		init(config);
	}
	
	private void init(final ConsumerServiceConfig config){
		this.config = config;
		if(config == null){
			throw new IllegalArgumentException("Missing ServiceConfig");
		}
		if(config.getTopic() == null || "".equals(config.getTopic())){
			throw new IllegalArgumentException("topic required");
		} 
		if(config.getBroker() == null){
			throw new IllegalArgumentException("broker required");
		} 
		
		if(config.getConsumerHandler() != null){
			consumerHandler = config.getConsumerHandler();
		} else {
			final MessageProcessor messageProcessor = config.getMessageProcessor();
			if(messageProcessor != null){
				//default consumer trying to route back message if processed
				consumerHandler = buildFromMessageProcessor(messageProcessor); 
			}  
		}
	}
	
	private ConsumerHandler buildFromMessageProcessor(final MessageProcessor messageProcessor){
		return new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				if(config.isVerbose()){
					log.info("Request:\n"+msg);
				}
				final String mq = msg.getTopic();
				final String msgId  = msg.getId();
				final String sender = msg.getSender();
				
				Message res = messageProcessor.process(msg);
				
				if(res != null){
					res.setId(msgId);
					res.setTopic(mq);  
					res.setReceiver(sender); 
					if(config.isVerbose()){
						log.info("Response:\n"+res);
					}
					//route back message
					consumer.reply(res);
				}
			}
		};
	} 
	 
	@Override
	public void close() throws IOException {
		if(this.consumerThreadGroupMap != null){ 
			for(ConsumerThreadGroup consumerThreadGroup : consumerThreadGroupMap.values()){
				consumerThreadGroup.close();
			}
			this.consumerThreadGroupMap.clear();
			this.consumerThreadGroupMap = null;
		} 
		if(consumerExecutor != null){
			consumerExecutor.shutdown();
			consumerExecutor = null;
		}
	} 
	
	private void addConsumerThreadGroup(Broker broker, MqConfig config, int consumerCount){
		if(consumerThreadGroupMap.containsKey(broker.brokerAddress())) return;
		
		MqConfig mqConfig = config.clone();
		mqConfig.setBroker(broker); 
		ConsumerThreadGroup consumerThreadGroup = new ConsumerThreadGroup(consumerCount, mqConfig);
		consumerThreadGroup.start(); 
		consumerThreadGroupMap.put(broker.brokerAddress(), consumerThreadGroup);
	}
	
	private void removeConsumerThreadGroup(String serverAddress){
		ConsumerThreadGroup consumerThreadGroup = consumerThreadGroupMap.remove(serverAddress);
		if(consumerThreadGroup == null){
			return;
		}
		
		try {
			consumerThreadGroup.close();
			consumerThreadGroupMap.remove(serverAddress);
		} catch (IOException e) {
			log.error(e.getMessage(), e);		
		}
	}
	
	
	public synchronized void start() throws IOException{ 
		if(isStarted) return;  
		if(this.consumerHandler == null){
			throw new IllegalArgumentException("MessageHandler and MessageProcessor are both null");
		}
		if(this.config.getBroker() == null){
			throw new IllegalArgumentException("Missing broker");
		}
		
		int n = config.getThreadPoolSize();
		consumerExecutor = new ThreadPoolExecutor(n, n, 120, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>(config.getMaxInFlightMessage()),
				new ThreadPoolExecutor.CallerRunsPolicy()); 
		
		Broker broker = config.getBroker(); 
		
		if(config.isParallelMode()){
			broker.addServerNotifyListener(new ServerNotifyListener() { 
				@Override
				public void onServerLeave(String serverAddress) { 
					removeConsumerThreadGroup(serverAddress);
				}
				
				@Override
				public void onServerJoin(Broker broker) { 
					addConsumerThreadGroup(broker, config, config.getConsumerCount());
				}
			});  
			for(Broker sbroker : broker.availableServerList()){
				addConsumerThreadGroup(sbroker, config, config.getConsumerCount());
			}
			
		} else {
			addConsumerThreadGroup(broker, config, config.getConsumerCount());
		} 
		
		isStarted = true;
	} 
	
	public void start(ConsumerHandler consumerHandler) throws IOException{
		onMessage(consumerHandler);
		start();
	}
	
	public void start(MessageProcessor messageProcessor) throws IOException{
		onMessage(messageProcessor);
		start();
	}
	
	public synchronized void onMessage(ConsumerHandler consumerHandler){
		this.consumerHandler = consumerHandler;
	}
	
	public synchronized void onMessage(MessageProcessor messageProcessor){
		this.consumerHandler = buildFromMessageProcessor(messageProcessor);
	}
	
	class ConsumerThreadGroup implements Closeable{
		private int consumerCount;
		private ConsumerThread[] threads;
		
		public ConsumerThreadGroup(int consumerCount, MqConfig config){
			this.consumerCount = consumerCount;
			threads = new ConsumerThread[consumerCount];
			for(int i=0;i<this.consumerCount;i++){
				threads[i] = new ConsumerThread(config);
			}
		}
		
		public void start(){
			for(Thread thread : threads){
				thread.start();
			}
		}
		
		public void close() throws IOException{
			for(ConsumerThread thread : threads){
				thread.close();
			}
		} 
	}
	
	class ConsumerThread extends Thread implements Closeable{
		Consumer consumer;
		public ConsumerThread(MqConfig config) {
			this.consumer = new Consumer(config); 
		}
		
		@Override
		public void run() { 
			while (true) {
				try {
					consumer.declareTopic();
					break;
				} catch (IOException e) {
					log.error(e.getMessage(), e); 
					continue;
				} catch (InterruptedException e) {
					return;
				}  
			}
			
			while (true) {
				try { 
					final Message msg;
					try {
						msg = consumer.take();
					} catch (InterruptedException e) {
						consumer.close(); 
						break;
					} catch (MqException e) { 
						throw e; 
					}  
					if (consumerHandler == null) {
						log.warn("Missing consumerHandler, call onMessage first");
						continue;
					}  
					consumerExecutor.submit(new Runnable() {
						@Override
						public void run() {
							try {
								consumerHandler.handle(msg, consumer);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}
					}); 
					
				} catch (IOException e) {  
					log.error(e.getMessage(), e); 
				}
			}
			
		}

		@Override
		public void close() throws IOException {
			interrupt(); 
			consumer.close();   
		} 
	}
}

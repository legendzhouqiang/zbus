package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class ConsumerService implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConsumerService.class); 
	private ConsumerServiceConfig config; 
	private Map<String, ConsumerThread[]> consumerThreadGroupMap = new ConcurrentHashMap<String, ConsumerThread[]>(); 
	private ThreadPoolExecutor consumerExecutor;  
	private ConsumerHandler consumerHandler; 
	private boolean isStarted = false; 
	 
	public ConsumerService(ConsumerServiceConfig config){ 
		init(config);
	}
	
	public ConsumerService(Broker broker, String topic){
		ConsumerServiceConfig config = new ConsumerServiceConfig(broker);
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
		
		if(config.getConsumerHandler() != null){
			consumerHandler = config.getConsumerHandler();
		} else {
			final MessageProcessor messageProcessor = config.getMessageProcessor();
			if(messageProcessor == null){
				throw new IllegalArgumentException("MessageHandler is null, and MessageProcessor must be provided");
			} 
			//default consumer trying to route back message if processed
			consumerHandler = buildFromMessageProcessor(messageProcessor);
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
			for(ConsumerThread[] consumerThreadGroup : consumerThreadGroupMap.values()){
				for(ConsumerThread consumerThread : consumerThreadGroup){
					consumerThread.close();
				}
			}
			this.consumerThreadGroupMap.clear();
			this.consumerThreadGroupMap = null;
		} 
		if(consumerExecutor != null){
			consumerExecutor.shutdown();
			consumerExecutor = null;
		}
	} 
	
	public synchronized void start() throws IOException{ 
		if(isStarted) return;  
		int n = config.getThreadPoolSize();
		consumerExecutor = new ThreadPoolExecutor(n, n, 120, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>(config.getMaxInFlightMessage()),
				new ThreadPoolExecutor.CallerRunsPolicy()); 
		 
		Broker[] brokers = config.getBrokers();
		int consumerCount = config.getConsumerCount();
		if(brokers.length < 1 || consumerCount < 1) return;
		 
		for(int i=0; i<brokers.length; i++){
			ConsumerThread[] consumerThreadGroup = new ConsumerThread[consumerCount]; 
			MqConfig mqConfig = config.clone();
			mqConfig.setBroker(brokers[i]); 
			
			String brokerId = ""+i; //TODO
			 
			for(int j=0; j<consumerCount; j++){   
				ConsumerThread thread = consumerThreadGroup[j] = new ConsumerThread(mqConfig); 
				thread.start();
			}
			consumerThreadGroupMap.put(brokerId, consumerThreadGroup);
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
						consumer = null;
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
			if(consumer != null){
				consumer.close();
				consumer = null;
			}
		} 
	}
}

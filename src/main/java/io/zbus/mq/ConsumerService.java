package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class ConsumerService implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConsumerService.class); 
	private ConsumerServiceConfig config; 
	private Consumer[][] consumers; 
	private boolean isStarted = false; 
	private ThreadPoolExecutor executor; 
	 
	public ConsumerService(ConsumerServiceConfig config){
		this.config = config; 
	}
	 
	@Override
	public void close() throws IOException {
		if(this.consumers != null){
			for(Consumer[] consumerList : this.consumers){
				for(Consumer consumer : consumerList){
					consumer.stop();
				}
			}
		} 
		if(executor != null){
			executor.shutdown();
		}
	}
	
	public void start() throws IOException{ 
		if(isStarted) return;
		if(config == null){
			throw new IllegalArgumentException("Missing ServiceConfig");
		}
		if(config.getTopic() == null || "".equals(config.getTopic())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getMessageProcessor() == null && config.getConsumerHandler() == null){
			throw new IllegalArgumentException("ConsumerHandler or MessageProcessor required");
		}   
		 
		int n = config.getThreadPoolSize();
		executor = new ThreadPoolExecutor(n, n, 120, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>(config.getMaxInFlightMessage()),
				new ThreadPoolExecutor.CallerRunsPolicy()); 
		
		final MessageProcessor processor = config.getMessageProcessor();
		Broker[] brokers = config.getBrokers();
		int consumerCount = config.getConsumerCount();
		if(brokers.length < 1 || consumerCount < 1) return;
		
		this.consumers = new Consumer[brokers.length][];
		for(int i=0; i<consumers.length; i++){
			Consumer[] consumerGroup = consumers[i] = new Consumer[consumerCount];
			
			MqConfig mqConfig = new MqConfig();
			mqConfig.setBroker(brokers[i]);
			mqConfig.setTopic(config.getTopic()); 
			mqConfig.setVerbose(config.isVerbose());
			mqConfig.setAppid(config.getAppid()); 
			mqConfig.setToken(config.getToken()); 
			
			ConsumerHandler handler = config.getConsumerHandler();
			for(int j=0; j<consumerGroup.length; j++){  
				Consumer c = consumerGroup[j] = new Consumer(mqConfig); 
				
				c.setConsumeExecutor(executor);
				c.setConsumeInPool(true); 
				
				if(handler == null){
					handler = new ConsumerHandler() { 
						@Override
						public void handle(Message msg, Consumer consumer) throws IOException { 
							if(config.isVerbose()){
								log.info("Request:\n"+msg);
							}
							final String mq = msg.getTopic();
							final String msgId  = msg.getId();
							final String sender = msg.getSender();
							Message res = processor.process(msg);
							
							if(res != null){
								res.setId(msgId);
								res.setTopic(mq);  
								res.setReceiver(sender); 
								if(config.isVerbose()){
									log.info("Response:\n"+res);
								}
								//route back message
								consumer.routeMessage(res);
							}
						}
					};
				}
				c.onMessage(handler);
			}
		}
		
		for(int i=0; i<consumers.length; i++){
			Consumer[] consumerGroup = consumers[i];
			for(int j=0; j<consumerGroup.length; j++){  
				Consumer c = consumerGroup[j];
				c.start();
			}
		}
		
		isStarted = true;
	} 
}

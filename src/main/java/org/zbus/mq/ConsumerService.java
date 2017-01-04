package org.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;

public class ConsumerService implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConsumerService.class); 
	protected ConsumerServiceConfig config; 
	private Consumer[][] consumers; 
	private boolean isStarted = false; 
	private ThreadPoolExecutor executor; 
	
	public ConsumerService(){
		
	}
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
		if(config.getMq() == null || "".equals(config.getMq())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getMessageProcessor() == null && config.getConsumerHandler() == null){
			throw new IllegalArgumentException("ConsumerHandler or MessageProcessor required");
		}  
		
		
		if(config.isConsumerHandlerRunInPool()){
			int n = config.getConsumerHandlerPoolSize();
			executor = new ThreadPoolExecutor(n, n, 120, TimeUnit.SECONDS, 
					new LinkedBlockingQueue<Runnable>(config.getInFlightMessageCount()),
					new ThreadPoolExecutor.CallerRunsPolicy());
		}
		
		final MessageProcessor processor = config.getMessageProcessor();
		Broker[] brokers = config.getBrokers();
		int consumerCount = config.getConsumerCount();
		if(brokers.length < 1 || consumerCount < 1) return;
		
		this.consumers = new Consumer[brokers.length][];
		for(int i=0; i<consumers.length; i++){
			Consumer[] consumerGroup = consumers[i] = new Consumer[consumerCount];
			
			MqConfig mqConfig = new MqConfig();
			mqConfig.setBroker(brokers[i]);
			mqConfig.setMq(config.getMq()); 
			mqConfig.setVerbose(config.isVerbose());
			mqConfig.setAppid(config.getAppid()); 
			mqConfig.setToken(config.getToken()); 
			
			ConsumerHandler handler = config.getConsumerHandler();
			for(int j=0; j<consumerGroup.length; j++){  
				Consumer c = consumerGroup[j] = new Consumer(mqConfig); 
				if(config.isConsumerHandlerRunInPool()){
					c.setConsumerHandlerExecutor(executor);
					c.setConsumerHandlerRunInPool(true);
				}
				
				if(handler == null){
					handler = new ConsumerHandler() { 
						@Override
						public void handle(Message msg, Consumer consumer) throws IOException { 
							if(config.isVerbose()){
								log.info("Request:\n"+msg);
							}
							final String mq = msg.getMq();
							final String msgId  = msg.getId();
							final String sender = msg.getSender();
							Message res = processor.process(msg);
							
							if(res != null){
								res.setId(msgId);
								res.setMq(mq);  
								res.setRecver(sender); 
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

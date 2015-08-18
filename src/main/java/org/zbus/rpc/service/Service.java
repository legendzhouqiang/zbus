package org.zbus.rpc.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.mq.Broker;
import org.zbus.mq.Consumer;
import org.zbus.mq.MqConfig;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageHandler;

public class Service implements Closeable {   
	private final ServiceConfig config; 
	private Consumer[][] consumerGroups;
	private final ThreadPoolExecutor threadPoolExecutor;
	private boolean isStarted = false;
	public Service(ServiceConfig config){
		this.config = config;
		if(config.getMq() == null || "".equals(config.getMq())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getServiceHandler() == null){
			throw new IllegalArgumentException("ServiceHandler required");
		}  
		threadPoolExecutor = new ThreadPoolExecutor(config.getThreadCount(),
				config.getThreadCount(), 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	 
	@Override
	public void close() throws IOException {
		if(this.consumerGroups != null){
			for(Consumer[] consumerGroup : this.consumerGroups){
				for(Consumer consumer : consumerGroup){
					consumer.stop();
				}
			}
		} 
	}
	
	public void start() throws IOException{ 
		if(isStarted) return;
		
		final ServiceHandler serviceHandler = config.getServiceHandler();
		Broker[] brokers = config.getBrokers();
		int consumerCount = config.getConsumerCount();
		if(brokers.length < 1 || consumerCount < 1) return;
		
		this.consumerGroups = new Consumer[brokers.length][];
		for(int i=0; i<consumerGroups.length; i++){
			Consumer[] consumerGroup = consumerGroups[i] = new Consumer[consumerCount];
			
			MqConfig mqConfig = new MqConfig();
			mqConfig.setBroker(brokers[i]);
			mqConfig.setMq(config.getMq());
			mqConfig.setMode(config.getMode());
			mqConfig.setTopic(config.getTopic());
			mqConfig.setVerbose(config.isVerbose());
			
			for(int j=0; j<consumerGroup.length; j++){  
				
				final Consumer c = consumerGroup[j] = new Consumer(mqConfig);
				c.onMessage(new MessageHandler() { 
					@Override
					public void handle(Message msg, Session sess) throws IOException { 
						final String mq = msg.getMq();
						final String msgId  = msg.getId();
						final String sender = msg.getSender();
						Message res = serviceHandler.handleRequest(msg);
						res.setId(msgId);
						res.setMq(mq);  
						res.setRecver(sender); 
						//route back message
						c.routeMessage(res);
					}
				}, threadPoolExecutor);
			}
		}
		
		for(int i=0; i<consumerGroups.length; i++){
			Consumer[] consumerGroup = consumerGroups[i];
			for(int j=0; j<consumerGroup.length; j++){  
				Consumer c = consumerGroup[j];
				c.start();
			}
		}
		
		isStarted = true;
	} 
}
/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.rpc.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.MqConfig;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;

public class Service implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Service.class); 
	protected ServiceConfig config; 
	private Consumer[][] consumerGroups; 
	private boolean isStarted = false; 
	private ThreadPoolExecutor executor; 
	
	public Service(){
		
	}
	public Service(ServiceConfig config){
		this.config = config; 
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
		
		this.consumerGroups = new Consumer[brokers.length][];
		for(int i=0; i<consumerGroups.length; i++){
			Consumer[] consumerGroup = consumerGroups[i] = new Consumer[consumerCount];
			
			MqConfig mqConfig = new MqConfig();
			mqConfig.setBroker(brokers[i]);
			mqConfig.setMq(config.getMq()); 
			mqConfig.setVerbose(config.isVerbose());
			mqConfig.setAccessToken(config.getAccessToken());
			mqConfig.setRegisterToken(config.getRegisterToken());
			
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
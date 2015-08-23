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
package org.zbus.rpc.broking;

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
import org.zbus.net.http.MessageProcessor;

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
		
		final MessageProcessor serviceHandler = config.getServiceHandler();
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
						Message res = serviceHandler.process(msg);
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
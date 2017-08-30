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

import org.zbus.broker.Broker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.MqConfig;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;

public class Service implements Closeable {   
	private final ServiceConfig config; 
	private Consumer[][] consumerGroups; 
	private boolean isStarted = false;
	public Service(ServiceConfig config){
		this.config = config;
		if(config.getMq() == null || "".equals(config.getMq())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getMessageProcessor() == null && config.getConsumerHandler() == null){
			throw new IllegalArgumentException("ConsumerHandler or MessageProcessor required");
		}  
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
			mqConfig.setMode(config.getMode());
			mqConfig.setTopic(config.getTopic());
			mqConfig.setVerbose(config.isVerbose());
			
			ConsumerHandler handler = config.getConsumerHandler();
			for(int j=0; j<consumerGroup.length; j++){  
				Consumer c = consumerGroup[j] = new Consumer(mqConfig); 
				if(handler == null){
					handler = new ConsumerHandler() { 
						@Override
						public void handle(Message msg, Consumer consumer) throws IOException { 
							final String mq = msg.getMq();
							final String msgId  = msg.getId();
							final String sender = msg.getSender();
							Message res = processor.process(msg);
							
							if(res != null){
								res.setId(msgId);
								res.setMq(mq);  
								res.setRecver(sender); 
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
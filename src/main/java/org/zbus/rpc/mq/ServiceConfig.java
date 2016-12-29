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

import org.zbus.broker.Broker;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.MqConfig;
import org.zbus.net.http.Message.MessageProcessor;

public class ServiceConfig extends MqConfig { 
	//capable of control underlying Session, priority over processor
	//if consumerHandler is set, messageProcessor will be disabled
	private ConsumerHandler consumerHandler; 
	private MessageProcessor messageProcessor; 
	private int consumerCount = 4; 
	private boolean consumerHandlerRunInPool = true;
	private int consumerHandlerPoolSize = 64; 
	private int inFlightMessageCount = 100;
	private boolean verbose = false;
	private Broker[] brokers;
 
	public ServiceConfig(Broker... brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		} 
	}

	public void setBrokers(Broker[] brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		}
		
	}

	public Broker[] getBrokers() {
		if (brokers == null || brokers.length == 0) {
			if (getBroker() != null) {
				brokers = new Broker[] { getBroker() };
			}
		}
		return this.brokers;
	}

	public int getConsumerCount() {
		return consumerCount;
	}

	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	}

	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}

	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	} 
	
	
	public ConsumerHandler getConsumerHandler() {
		return consumerHandler;
	}

	public void setConsumerHandler(ConsumerHandler consumerHandler) {
		this.consumerHandler = consumerHandler;
	} 
	
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
	
	
	public int getConsumerHandlerPoolSize() {
		return consumerHandlerPoolSize;
	}

	public void setConsumerHandlerPoolSize(int consumerHandlerPoolSize) {
		this.consumerHandlerPoolSize = consumerHandlerPoolSize;
	} 

	public boolean isConsumerHandlerRunInPool() {
		return consumerHandlerRunInPool;
	}

	public void setConsumerHandlerRunInPool(boolean consumerHandlerRunInPool) {
		this.consumerHandlerRunInPool = consumerHandlerRunInPool;
	}
	
	

	public int getInFlightMessageCount() {
		return inFlightMessageCount;
	}

	public void setInFlightMessageCount(int inFlightMessageCount) {
		this.inFlightMessageCount = inFlightMessageCount;
	}

	@Override
	public ServiceConfig clone() {
		return (ServiceConfig) super.clone();
	}

}

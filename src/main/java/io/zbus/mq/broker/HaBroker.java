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
package io.zbus.mq.broker;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.net.MessageClient;
import io.zbus.mq.tracker.DefaultBrokerSelector;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class HaBroker implements Broker {   
	private static final Logger log = LoggerFactory.getLogger(HaBroker.class);
	
	BrokerSelector brokerSelector; 
	final boolean ownBrokerSelector;
	
	public HaBroker(BrokerConfig config) throws IOException{ 
		this.brokerSelector = config.brokerSelector;
		if(this.brokerSelector == null){
			this.brokerSelector = new DefaultBrokerSelector(config);
			ownBrokerSelector = true;
		} else {
			ownBrokerSelector = false;
		} 
	}
	
	public HaBroker(BrokerSelector brokerSelector, BrokerConfig config) throws IOException{ 
		this.brokerSelector = brokerSelector;
		ownBrokerSelector = false;
	}
	
	@Override
	public MessageInvoker selectInvoker(String mq) throws IOException { 
		Broker broker = brokerSelector.selectByBrokerHint(mq);
		if(broker == null){
			throw new BrokerException("Missing broker for " + mq);
		}
		return broker.selectInvoker(mq);
	}

	@Override
	public void releaseInvoker(MessageInvoker client) throws IOException { 
		if(!(client instanceof MessageClient)){
			throw new IllegalArgumentException("client should be instance of MessageClient");
		}
		
		MessageClient messageClient = (MessageClient)client;
		Broker broker = brokerSelector.selectByClient(messageClient);
		if(broker == null){
			log.warn("Missing broker for " + messageClient);
			messageClient.close();
		} else {
			broker.releaseInvoker(messageClient); 
		}
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		List<Broker> brokerList = brokerSelector.selectByRequestMsg(req);
		if(brokerList == null || brokerList.size() == 0){
			throw new BrokerException("Missing broker for " + req);
		} 
		Message res = null;
		for(Broker broker : brokerList){
			res = broker.invokeSync(req, timeout);
		}
		return res;
	}

	@Override
	public void invokeAsync(Message req, MessageCallback callback)
			throws IOException { 
		List<Broker> brokerList = brokerSelector.selectByRequestMsg(req);
		if(brokerList == null || brokerList.size() == 0){
			throw new BrokerException("Missing broker for " + req);
		}  
		for(Broker broker : brokerList){
			broker.invokeAsync(req, callback);
		}  
	}

	@Override
	public void close() throws IOException { 
		if(ownBrokerSelector){
			brokerSelector.close();
		}
	} 
	
	
	/**
	 * Broker selector interface for HA broker, default implementation is DefaultBrokerSelector
	 * 
	 * @author HONG LEIMING 
	 *
	 */
	public static interface BrokerSelector extends Closeable{
		/**
		 * Select single broker based on mq entry
		 * @param hint
		 * @return best broker matched for the hint
		 */
		Broker selectByBrokerHint(String mq);
		/**
		 * Select best broker(s) based on the request message content
		 * Criteria could be Server/EntryId(MQ) etc.
		 * @param msg
		 * @return List of brokers for PubSub, list of single broker otherwise
		 */
		List<Broker> selectByRequestMsg(Message msg);
		/**
		 * Provide a mechanism to resolve the entry abstraction in a Message
		 * @param msg
		 * @return
		 */
		String getEntry(Message msg);
		/**
		 * Select a broker based on the MessageClient.
		 * This is usually implemented by tagging a client when being generated.
		 * 
		 * @param client
		 * @return
		 */
		Broker selectByClient(MessageClient client);
	}
}


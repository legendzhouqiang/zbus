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
package org.zbus.broker;

import java.io.IOException;
import java.util.List;

import org.zbus.broker.ha.DefaultBrokerSelector;
import org.zbus.kit.log.Logger;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.http.MessageClient;

public class HaBroker implements Broker {   
	private static final Logger log = Logger.getLogger(HaBroker.class);
	
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
	public MessageInvoker getInvoker(BrokerHint hint) throws IOException { 
		Broker broker = brokerSelector.selectByBrokerHint(hint);
		if(broker == null){
			throw new BrokerException("Missing broker for " + hint);
		}
		return broker.getInvoker(hint);
	}

	@Override
	public void closeInvoker(MessageInvoker client) throws IOException { 
		if(!(client instanceof MessageClient)){
			throw new IllegalArgumentException("client should be instance of MessageClient");
		}
		
		MessageClient messageClient = (MessageClient)client;
		Broker broker = brokerSelector.selectByClient(messageClient);
		if(broker == null){
			log.warn("Missing broker for " + messageClient);
			messageClient.close();
		} else {
			broker.closeInvoker(messageClient); 
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
	public void invokeAsync(Message req, ResultCallback<Message> callback)
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

	@Override
	public Message invokeSync(Message req) throws IOException, InterruptedException {
		return invokeSync(req, 10000);
	} 
}


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

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.MqException;
import io.zbus.mq.Protocol;
import io.zbus.mq.net.MessageClient;
import io.zbus.mq.net.MessageClientFactory;
import io.zbus.net.EventDriver;
import io.zbus.net.Pool;

public class SingleBroker implements Broker {   
	private BrokerConfig config; 
	private EventDriver eventDriver;
	private boolean ownEventDriver = false;
	
	private final Pool<MessageClient> pool; 
	private final MessageClientFactory factory;  
	
	public SingleBroker() throws IOException{
		this(new BrokerConfig());
	}
	
	public SingleBroker(BrokerConfig config) throws IOException{ 
		this.config = config;
		this.eventDriver = config.getEventDriver();
		if(this.eventDriver == null){
			this.eventDriver = new EventDriver();
			this.ownEventDriver = true;
		}
		this.factory = new MessageClientFactory(this.config.getBrokerAddress(),eventDriver);
		this.pool = new Pool<MessageClient>(factory, config.getConnectionPoolSize());
	}  

	@Override
	public void close() throws IOException { 
		this.pool.close(); 
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	}  
	 
	@Override
	public MessageInvoker selectForProducer(String topic) throws IOException{ 
		try {
			MessageClient client = this.pool.borrowObject(); 
			client.attr(Protocol.SERVER, factory.getServerAddress());
			client.attr("type", "producer");
			return client;
		} catch (Exception e) {
			throw new MqException(e.getMessage(), e);
		} 
	}
	
	@Override
	public MessageInvoker selectForConsumer(String topic) throws IOException{ 
		MessageClient client = factory.createObject();
		client.attr(Protocol.SERVER, factory.getServerAddress());
		client.attr("type", "consumer");
		return client;
	}

	public void releaseInvoker(MessageInvoker messageInvoker) throws IOException {
		if(messageInvoker == null) return; //ignore 
		if(!(messageInvoker instanceof MessageClient)){
			throw new IllegalArgumentException("releaseInvoker should accept MessageClient");
		} 
		MessageClient client = (MessageClient)messageInvoker;
		if("consumer".equals(client.attr("type"))){
			client.close();
			return;
		}
		this.pool.returnObject(client); 
	}
}




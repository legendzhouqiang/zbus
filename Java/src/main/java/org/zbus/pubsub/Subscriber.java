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
package org.zbus.pubsub;

import java.io.Closeable;
import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.MqAdmin;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Protocol;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.http.MessageClient;

public class Subscriber extends MqAdmin implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Subscriber.class); 
	private MessageInvoker client;  
	private String topic = null;   
	private volatile MessageHandler messageHandler;  
	
	public Subscriber(Broker broker, String mq) {
		super(broker, mq, MqMode.PubSub);
	}

	public Subscriber(MqConfig config) {
		super(config);
		this.topic = config.getTopic();
	}

	private BrokerHint brokerHint() {
		BrokerHint hint = new BrokerHint();
		hint.setEntry(this.mq);
		return hint;
	}  
	 
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) { 
		this.topic = topic;
	} 
	
	private MessageClient ensureClient() throws IOException{
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.getInvoker(brokerHint());
			} 
		}  
		return (MessageClient)this.client;
	}
	
	public void subscribe(String topic) throws IOException{   
		//TODO change MessageClient requirement
		MessageClient messageClient = ensureClient();
		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setMq(mq);
		req.setHead("token", accessToken); 
		req.setTopic(topic);
		
		try {
			messageClient.invokeSync(req);
		} catch (InterruptedException e) {
			//ignore
		}
	}  
	  
	public void onMessage(final MessageHandler handler) throws IOException { 
		this.messageHandler = handler;
		if(this.messageHandler == null){
			return; //just ignore it
		}  

		MessageClient messageClient = ensureClient();
		messageClient.onMessage(messageHandler);  
		messageClient.onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() throws IOException { 
				log.info("Connected, try to create mq and subscribe on topic: " + topic);
				try {
					createMQ();
				} catch (InterruptedException e) {
					//ignore
				}
				subscribe(topic);
			}
		}); 
		messageClient.ensureConnectedAsync();
	}
	 
	public void close() throws IOException { 
		try {
			if (this.client != null) {
				this.broker.closeInvoker(this.client);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} 
	} 
	
	@Override
	protected Message invokeSync(Message req) throws IOException, InterruptedException { 
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.getInvoker(brokerHint());
			}
			return client.invokeSync(req, 10000);
		} 
	} 
}

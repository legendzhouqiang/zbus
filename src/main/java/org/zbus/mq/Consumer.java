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
package org.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.zbus.broker.Broker;
import org.zbus.kit.log.Logger;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = Logger.getLogger(Consumer.class);

	private MessageClient client; // 消费者拥有一个物理链接
	private String topic = null; // 为发布订阅者的主题，当Consumer的模式为发布订阅时候起作用
	private int consumeTimeout = 10000; //ms
	public Consumer(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	}

	public Consumer(MqConfig config) {
		super(config);
		this.topic = config.getTopic();
	}

	private void ensureClient() throws IOException{
		if(this.client == null){
    		synchronized (this) {
				if(this.client == null){
					this.client = broker.getClient(brokerHint()); 
				}
			} 
    	}
	}
	
	public Message recv(int timeout) throws IOException, InterruptedException {
		ensureClient();
		
		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setMq(mq);
		if (MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			if (this.topic != null) {
				req.setTopic(this.topic);
			}
		}

		Message res = null;
		try {
			res = client.invokeSync(req, timeout);
			if (res != null && res.isStatus404()) {
				if (!this.createMQ()) {
					throw new IllegalStateException("register error");
				}
				return recv(timeout);
			}
			if(res != null){
				res.setId(res.getRawId());
				res.removeHead(Message.RAWID);
			}
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
			try {
				broker.closeClient(client);
				client = broker.getClient(brokerHint());
			} catch (IOException ex) {
				log.error(e.getMessage(), e);
			}
		}
		return res;
	}

	public void close() throws IOException {
		stop();
		if (this.client != null) {
			this.broker.closeClient(this.client);
		} 
	}

	@Override
	protected Message invokeCreateMQ(Message req) throws IOException,
			InterruptedException {
		ensureClient();
		return client.invokeSync(req);
	}
	
	public void routeMessage(Message msg) throws IOException {
		ensureClient();
		msg.setCmd(Protocol.Route);
		msg.setAck(false); 
		
		client.send(msg);
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		if (!MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			throw new IllegalStateException("topic require PubSub mode");
		}
		this.topic = topic;
	}
	
	
	private Thread messageThread = null;
	public void onMessage(final MessageHandler handler) throws IOException{
		stop();
		
		Runnable task = new Runnable() {
			@Override
			public void run() { 
				for(;;){
					try {
						final Message msg;
						try { 
							msg = recv(consumeTimeout); 
						} catch (InterruptedException e) { 
							break;
						}
						if(msg == null) continue;
						
						try {
							handler.handle(msg, client.getSession());
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}  
					} catch (IOException e) { 
						log.error(e.getMessage(), e);
					}
				}
			}
		};
		
		messageThread = new Thread(task);  
	}
	
	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public void stop(){
		if(messageThread != null){
			messageThread.interrupt();
			messageThread = null;
		}
	}
	
	public void start(){
		if(messageThread == null) return;
		if(messageThread.isAlive()) return;
		messageThread.start();
	}
	
	
}

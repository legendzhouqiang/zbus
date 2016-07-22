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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.MsgHandler;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.http.MessageClient;

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class); 
	private MessageInvoker client;  
	private String topic = null;  
	private int consumeTimeout = 120000; // 2 minutes

	public Consumer(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	}

	public Consumer(MqConfig config) {
		super(config);
		this.topic = config.getTopic();
	}

	private BrokerHint brokerHint() {
		BrokerHint hint = new BrokerHint();
		hint.setEntry(this.mq);
		return hint;
	}

	public Message take(int timeout) throws IOException, InterruptedException {
		if (MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			throw new IllegalStateException("PubSub not support take");
		}
		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setMq(mq);
		req.setHead("token", accessToken); 

		Message res = null;
		try {  
			synchronized (this) {
				if (this.client == null) {
					this.client = broker.getInvoker(brokerHint());
				}
				res = client.invokeSync(req, timeout);
			} 
			if (res == null)
				return res;
			res.setId(res.getOriginId());
			res.removeHead(Message.ORIGIN_ID);
			if (res.isStatus200()){
				String originUrl = res.getOriginUrl();
				if(originUrl == null){
					originUrl = "/";
				} else {
					res.removeHead(Message.ORIGIN_URL);
				}
				res.setUrl(originUrl);
				return res;
			}

			if (res.isStatus404()) {
				if (!this.createMQ()) {
					throw new MqException(res.getBodyString());
				}
				return take(timeout);
			}
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			try {
				broker.closeInvoker(client); 
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			} finally{
				synchronized (this) {
					this.client = null;
				}
			}
		}
		return res;
	}

	public Message take() throws InterruptedException, IOException {
		while (true) {
			Message message = take(consumeTimeout);
			if (message == null)
				continue;
			return message; 
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
	
	@Override
	protected void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.getInvoker(brokerHint());
			}
			client.invokeAsync(req, callback);
		} 
	}

	 
	public void routeMessage(Message msg) throws IOException {
		msg.setCmd(Protocol.Route);
		msg.setAck(false); 
		
		if(msg.isResponse()){
			msg.setOriginStatus(msg.getStatus());
			msg.asRequest(); //must send back request message type
		}   
		
		client.invokeAsync(msg, null); 
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
	
	public void subscribe(String topic) throws IOException{ 
		if (!MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			throw new IllegalStateException("subscribe require PubSub mode");
		}
		
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.getInvoker(brokerHint());
			} 
		} 
		
		MessageClient messageClient = (MessageClient)this.client;
		Message req = new Message();
		req.setCmd(Protocol.Consume);
		req.setMq(mq);
		req.setHead("token", accessToken); 
		req.setTopic(topic);
		
		messageClient.invokeAsync(req, null); 
	}
	
	
	//The followings are all related to start consumer cycle in another thread
	private volatile Thread consumerThread = null;
	private volatile ConsumerHandler consumerHandler;
	private volatile ConsumerExceptionHandler consumerExceptionHandler;
	private int consumerHandlerPoolSize = 64;
	private int inFlightMessageCount = 64;
	private boolean consumerHandlerRunInPool = false;
	private ThreadPoolExecutor consumerHandlerExecutor;  
	private boolean ownConsumerHandlerExecutor = false;
	
	private void initConsumerHandlerPoolIfNeeded(){
		if(consumerHandlerRunInPool && consumerHandlerExecutor == null){
			consumerHandlerExecutor = new ThreadPoolExecutor(consumerHandlerPoolSize, 
					consumerHandlerPoolSize, 120, TimeUnit.SECONDS, 
					new LinkedBlockingQueue<Runnable>(inFlightMessageCount),
					new ThreadPoolExecutor.CallerRunsPolicy());
			ownConsumerHandlerExecutor = true;
		}
	}
	private final Runnable consumerTask = new Runnable() {
		@Override
		public void run() {
			initConsumerHandlerPoolIfNeeded(); 
			while (true) {
				try {
					final Message msg;
					try {
						msg = take();
					} catch (InterruptedException e) {
						Consumer.this.close();
						break;
					} catch (MqException e) {
						if(consumerExceptionHandler != null){
							consumerExceptionHandler.onException(e, Consumer.this);
							break;
						} 
						throw e; 
					} 
					if (consumerHandler == null) {
						log.warn("Missing consumerHandler, call onMessage first");
						continue;
					}
					
					if (consumerHandlerRunInPool && consumerHandlerExecutor != null) { 
						consumerHandlerExecutor.submit(new Runnable() {
							@Override
							public void run() {
								try {
									consumerHandler.handle(msg, Consumer.this);
								} catch (IOException e) {
									log.error(e.getMessage(), e);
								}
							}
						});
					} else {
						try {
							consumerHandler.handle(msg, Consumer.this);
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
					
				} catch (IOException e) { 
					if(consumerExceptionHandler != null){
						consumerExceptionHandler.onException(e, Consumer.this);
					} else {
						log.error(e.getMessage(), e);
					}
				}
			}
		}
	};

	public void onMessage(final ConsumerHandler handler) throws IOException { 
		this.consumerHandler = handler;
		if(this.consumerHandler == null){
			return; //just ignore it
		}
		if (!MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			return;
		}
		
		
		synchronized (this) {
			if (this.client == null) {
				this.client = broker.getInvoker(brokerHint());
			} 
		}  
		final MessageClient messageClient = (MessageClient)this.client;
		initConsumerHandlerPoolIfNeeded(); 
		messageClient.onMessage(new MsgHandler<Message>() { 
			@Override
			public void handle(final Message msg, Session session) throws IOException {
				if(consumerHandlerRunInPool && consumerHandlerExecutor != null){
					consumerHandlerExecutor.submit(new Runnable() { 
						@Override
						public void run() { 
							try {
								consumerHandler.handle(msg, Consumer.this);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}
					});
				} else {
					consumerHandler.handle(msg, Consumer.this);
				}
			}
		}); 
		
		messageClient.onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() throws IOException {  
				createMQAsync(new ResultCallback<Message>() { 
					public void onReturn(Message result) {
						try {
							subscribe(topic);
							log.info("Connected(%s), Subscribe on topic: %s", messageClient.getConnectedServerAddress(), topic);
						} catch (IOException e) {
							//ignore
						}
					}
				});  
			}
		});
		
		messageClient.ensureConnectedAsync();
	}
	
	public void onException(final ConsumerExceptionHandler handler) {
		this.consumerExceptionHandler = handler;
	}

	public void close() throws IOException {
		stop(); 
	}
	
	public void stop() {
		if (consumerThread != null) {
			consumerThread.interrupt();
			consumerThread = null;
		}
		if(ownConsumerHandlerExecutor && consumerHandlerExecutor != null){
			consumerHandlerExecutor.shutdown();
			consumerHandlerExecutor = null;
		}
		try {
			if (this.client != null) {
				this.broker.closeInvoker(this.client);
			} 
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} 
	}
	
	public synchronized void start(ConsumerHandler handler) throws IOException{
		onMessage(handler);
		start();
	}

	public synchronized void start() throws IOException {
		if (MqMode.isEnabled(this.mode, MqMode.PubSub)) {
			return;
		}
		
		if (consumerThread == null) {
			consumerThread = new Thread(consumerTask);
			consumerThread.setName("ConsumerThread");
		}

		if (consumerThread.isAlive())
			return;
		consumerThread.start();
	} 

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 
	
	public int getConsumerHandlerPoolSize() {
		return consumerHandlerPoolSize;
	}

	public void setConsumerHandlerPoolSize(int consumerHandlerPoolSize) {
		this.consumerHandlerPoolSize = consumerHandlerPoolSize; 
	}
	
	

	public int getInFlightMessageCount() {
		return inFlightMessageCount;
	}

	public void setInFlightMessageCount(int inFlightMessageCount) {
		this.inFlightMessageCount = inFlightMessageCount;
	}
 
	public boolean isConsumeHandlerRunInPool() {
		return consumerHandlerRunInPool;
	}

	public void setConsumerHandlerRunInPool(boolean consumerHandlerRunInPool) {
		this.consumerHandlerRunInPool = consumerHandlerRunInPool;
	} 

	public ThreadPoolExecutor getConsumeExecutor() {
		return consumerHandlerExecutor;
	}

	public void setConsumerHandlerExecutor(ThreadPoolExecutor consumerHandlerExecutor) {
		if(this.consumerHandlerExecutor != null && ownConsumerHandlerExecutor){
			this.consumerHandlerExecutor.shutdown();
		}
		this.consumerHandlerExecutor = consumerHandlerExecutor;
	} 

	public static interface ConsumerHandler{
		void handle(Message msg, Consumer consumer) throws IOException;
	}
	
	public static interface ConsumerExceptionHandler { 
		void onException(Exception e, Consumer consumer);   
	}
}

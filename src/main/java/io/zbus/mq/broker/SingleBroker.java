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

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.net.MessageClient;
import io.zbus.mq.net.MessageClientFactory;
import io.zbus.net.EventDriver;
import io.zbus.net.Pool;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class SingleBroker implements Broker {
	private static final Logger log = LoggerFactory.getLogger(SingleBroker.class);     
	
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
		this.pool = new Pool<MessageClient>(factory, config.getConnectionPoolMaxSize());
	}  

	@Override
	public void close() throws IOException { 
		this.pool.close(); 
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	}  
	
	public void invokeAsync(Message msg, MessageCallback callback) throws IOException {  
		MessageClient client = null;
		try {
			client = this.pool.borrowObject(); 
			client.invokeAsync(msg, callback);
		} catch(IOException e){
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new BrokerException(e.getMessage(), e);
		} finally{
			if(client != null){
				this.pool.returnObject(client);
			}
		}
	} 

	public Message invokeSync(Message req, int timeout) throws IOException {
		MessageClient client = null;
		try {
			client = this.pool.borrowObject(); 
			return client.invokeSync(req, timeout);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new BrokerException(e.getMessage(), e);
		} finally{
			if(client != null){
				this.pool.returnObject(client);
			}
		}
	}
	
	public MessageInvoker selectInvoker(String mq) throws IOException{ 
		try {
			MessageClient client = factory.createObject();
			client.attr("server", factory.getServerAddress());
			return client;
		} catch (Exception e) {
			throw new BrokerException(e.getMessage(), e);
		} 
	}

	public void releaseInvoker(MessageInvoker client) throws IOException {
		if(client == null) return; //ignore
		if(client instanceof Closeable){
			((Closeable)client).close();
		} 
	}
 
}




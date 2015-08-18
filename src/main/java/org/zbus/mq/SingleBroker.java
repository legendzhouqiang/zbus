package org.zbus.mq;

import java.io.IOException;

import org.zbus.log.Logger;
import org.zbus.net.ResultCallback;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
import org.zbus.pool.ObjectFactory;
import org.zbus.pool.Pool;

public class SingleBroker implements Broker {
	private static final Logger log = Logger.getLogger(SingleBroker.class);     
	
	private final Pool<MessageClient> pool; 
	private String brokerAddress; 
	
	private BrokerConfig config;
	private Dispatcher dispatcher = null;
	private boolean ownDispatcher = false;
	 
	public SingleBroker(BrokerConfig config) throws IOException{ 
		this.config = config;
		this.brokerAddress = config.getBrokerAddress(); 
		
		if(config.getDispatcher() == null){
			this.ownDispatcher = true;
			this.dispatcher = new Dispatcher();
			this.config.setDispatcher(dispatcher);
		} else {
			this.dispatcher = config.getDispatcher();
			this.ownDispatcher = false;
		}
		this.dispatcher.start(); 
		
		this.pool = Pool.getPool(new MessageClientFactory(), this.config); 
	} 
	 

	@Override
	public void close() throws IOException { 
		this.pool.close(); 
		if(ownDispatcher && this.dispatcher != null){
			try {
				this.dispatcher.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	
	public String getBrokerAddress() {
		return brokerAddress;
	}
 
	public void invokeAsync(Message msg, ResultCallback<Message> callback)
			throws IOException {  
		MessageClient client = null;
		try {
			client = this.pool.borrowObject(); 
			client.invokeAsync(msg, callback);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new MqException(e.getMessage(), e);
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
			throw new MqException(e.getMessage(), e);
		} finally{
			if(client != null){
				this.pool.returnObject(client);
			}
		}
	}
	public MessageClient getClient(ClientHint hint) throws IOException{ 
		return new MessageClient(this.brokerAddress, this.dispatcher);
	}

	public void closeClient(MessageClient client) throws IOException {
		client.close();
	}

	
	private class MessageClientFactory implements ObjectFactory<MessageClient> {		
		@Override
		public boolean validateObject(MessageClient client) { 
			return client.hasConnected();
		}
		
		@Override
		public void destroyObject(MessageClient client){ 
			try {
				client.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				//ignore
			}
		}
		
		@Override
		public MessageClient createObject() { 
			return new MessageClient(brokerAddress, dispatcher); 
		}
	}
}




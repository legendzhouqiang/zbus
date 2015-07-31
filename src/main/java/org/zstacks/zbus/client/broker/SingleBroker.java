package org.zstacks.zbus.client.broker;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.ClientHint;
import org.zstacks.zbus.client.ZbusException;
import org.zstacks.znet.Message;
import org.zstacks.znet.RemotingClient;
import org.zstacks.znet.log.Logger;
import org.zstacks.znet.nio.Dispatcher;
import org.zstacks.znet.pool.RemotingClientPool;
import org.zstacks.znet.ticket.ResultCallback;

public class SingleBroker implements Broker {
	private static final Logger log = Logger.getLogger(SingleBroker.class);     
	private RemotingClientPool pool; 
	private String brokerAddress; 
	
	private SingleBrokerConfig config;
	private Dispatcher dispatcher = null;
	private boolean ownDispatcher = false;
	 
	public SingleBroker(SingleBrokerConfig config) throws IOException{ 
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
		
		this.pool = new RemotingClientPool(this.config); 
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
 
	public void invokeAsync(Message msg, ResultCallback callback)
			throws IOException {  
		RemotingClient client = null;
		try {
			client = this.pool.borrowObject(); 
			client.invokeAsync(msg, callback);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ZbusException(e.getMessage(), e);
		} finally{
			if(client != null){
				this.pool.returnObject(client);
			}
		}
	} 

	public Message invokeSync(Message req, int timeout) throws IOException {
		RemotingClient client = null;
		try {
			client = this.pool.borrowObject(); 
			return client.invokeSync(req, timeout);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ZbusException(e.getMessage(), e);
		} finally{
			if(client != null){
				this.pool.returnObject(client);
			}
		}
	}
	public RemotingClient getClient(ClientHint hint) throws IOException{ 
		return new RemotingClient(this.brokerAddress, this.dispatcher);
	}

	public void closeClient(RemotingClient client) throws IOException {
		client.close();
	}

}




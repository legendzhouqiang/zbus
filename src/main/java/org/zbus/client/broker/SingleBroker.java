package org.zbus.client.broker;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.client.Broker;
import org.zbus.client.ClientHint;
import org.zbus.client.ZbusException;
import org.zbus.remoting.ClientDispatcherManager;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.pool.RemotingClientPool;
import org.zbus.remoting.ticket.ResultCallback;

public class SingleBroker implements Broker {
	private static final Logger log = LoggerFactory.getLogger(SingleBroker.class);     
	private RemotingClientPool pool; 
	private String brokerAddress; 
	private ClientDispatcherManager clientDispatcherManager;
	 
	public SingleBroker(SingleBrokerConfig config) throws IOException{ 
		this.brokerAddress = config.getBrokerAddress(); 
		try {
			this.pool = new RemotingClientPool(config);
			this.clientDispatcherManager = this.pool.getClientDispatcherManager();
		} catch (IOException e) { 
			log.error(e.getMessage(),e);
		}
	} 
	  
	public void destroy() { 
		this.pool.close(); 
	}

	public String getBrokerAddress() {
		return brokerAddress;
	}
 
	public void invokeAsync(Message msg, ResultCallback callback)
			throws IOException {  
		RemotingClient client = null;
		try {
			client = this.pool.borrowObject();
			if(client.attr("broker") == null){
				client.attr("broker", brokerAddress);
			}
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

	@Override
	public Message invokeSync(Message req, int timeout) throws IOException {
		RemotingClient client = null;
		try {
			client = this.pool.borrowObject();
			if(client.attr("broker") == null){
				client.attr("broker", brokerAddress);
			}
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
	@Override
	public RemotingClient getClient(ClientHint hint) throws IOException{ 
		return new RemotingClient(this.brokerAddress, this.clientDispatcherManager);
	}


	@Override
	public void closeClient(RemotingClient client) throws IOException {
		client.close();
	}

}




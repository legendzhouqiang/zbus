package org.zbus.client.broker;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.ClientHint;
import org.zbus.client.ZbusException;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.RemotingClient;

public class SingleBroker implements Broker {
	private static final Logger log = LoggerFactory.getLogger(SingleBroker.class);     
	private RemotingClientPool pool; 
	private String brokerAddress;
	
	public SingleBroker(SingleBrokerConfig config){ 
		this.brokerAddress = config.getBrokerAddress();
		try {
			this.pool = new RemotingClientPool(this.brokerAddress, config.getPoolConfig());
		} catch (IOException e) { 
			log.error(e.getMessage(),e);
		}
	} 
	
	@Override
	public RemotingClient getClient(ClientHint hint) { 
		//Single broker ignore Hint
		try {
			RemotingClient client = this.pool.borrowObject();
			if(client != null){
				if(client.attr("broker") == null){
					client.attr("broker", this.brokerAddress);
				}
			}
			return client;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ZbusException(e.getMessage(), e);
		} 
	}

	@Override
	public void closeClient(RemotingClient client) { 
		if(!client.hasConnected()){
			try {
				this.pool.invalidateObject(client);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new ZbusException(e.getMessage(), e);
			}
		} else {
			this.pool.returnObject(client);
		}
	}
	
	@Override
	public void destroy() {  
		this.pool.close();
	}

	public String getBrokerAddress() {
		return brokerAddress;
	}
}




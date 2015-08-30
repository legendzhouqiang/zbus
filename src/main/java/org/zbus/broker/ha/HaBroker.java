package org.zbus.broker.ha;

import java.io.IOException;
import java.util.List;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.BrokerException;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class HaBroker implements Broker {   
	final BrokerSelector brokerSelector; 
	final boolean ownBrokerSelector;
	
	public HaBroker(BrokerConfig config) throws IOException{ 
		this.brokerSelector = new DefaultBrokerSelector(config);
		ownBrokerSelector = true;
	}
	
	public HaBroker(BrokerSelector brokerSelector, BrokerConfig config) throws IOException{ 
		this.brokerSelector = brokerSelector;
		ownBrokerSelector = false;
	}
	
	@Override
	public MessageClient getClient(ClientHint hint) throws IOException { 
		Broker broker = brokerSelector.selectByClientHint(hint);
		if(broker == null){
			throw new BrokerException("Missing broker for " + hint);
		}
		return broker.getClient(hint);
	}

	@Override
	public void closeClient(MessageClient client) throws IOException { 
		Broker broker = brokerSelector.selectByClient(client);
		if(broker == null){
			throw new BrokerException("Missing broker for " + client);
		} 
		broker.closeClient(client); 
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
}

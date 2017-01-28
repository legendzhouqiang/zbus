package io.zbus.mq.broker;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
	private final Pool<MessageClient> pool; 
	private final MessageClientFactory factory;  
	private final BrokerConfig config;
	private final boolean ownEventDriver;  
	private EventDriver eventDriver; 
	
	public SingleBroker(BrokerConfig config, EventDriver eventDriver) throws IOException{ 
		this.config = config.clone();
		this.eventDriver = eventDriver; 
		this.ownEventDriver = false; 
		
		this.factory = new MessageClientFactory(this.config.getBrokerAddress(), eventDriver);
		this.pool = new Pool<MessageClient>(factory, this.config.getConnectionPoolSize());
	}  
	 
	public SingleBroker(BrokerConfig config) throws IOException{
		this.config = config.clone();
		this.ownEventDriver = true;
		this.eventDriver = new EventDriver(); 
		
		this.factory = new MessageClientFactory(this.config.getBrokerAddress(), eventDriver);
		this.pool = new Pool<MessageClient>(factory, this.config.getConnectionPoolSize());
	}
	
	@Override
	public String brokerAddress() { 
		return this.config.getBrokerAddress();
	}
	
	@Override
	public void close() throws IOException { 
		this.pool.close(); 
		if(eventDriver != null && this.ownEventDriver){
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
	
	
	@Override
	public List<Broker> availableServerList() {
		return Arrays.asList((Broker)this);
	}

	
	@Override
	public void setServerSelector(ServerSelector selector) { 
		//ignore
	}

	@Override
	public void registerServer(String serverAddress) throws IOException { 
		//ignore
	}

	@Override
	public void unregisterServer(String serverAddress) throws IOException { 
		//ignore
	}

	@Override
	public void addServerListener(ServerNotifyListener listener) { 
		//ignore
	}

	@Override
	public void removeServerListener(ServerNotifyListener listener) { 
		//ignore
	} 
}




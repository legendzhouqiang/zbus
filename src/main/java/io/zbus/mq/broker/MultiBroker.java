package io.zbus.mq.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.MqException;
import io.zbus.mq.Protocol;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.EventDriver;

public class MultiBroker implements Broker {
	private Map<String, Broker> brokerMap = new ConcurrentHashMap<String, Broker>();
	private ServerSelector serverSelector = new DefaultServerSelector(); 
	private ServerTable serverTable = new ServerTable();
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>();
	
	private EventDriver eventDriver; 
	private final BrokerConfig config;
	 
	
	public MultiBroker(BrokerConfig config) throws IOException{ 
		this.config = config.clone();
		this.eventDriver = new EventDriver();
		
		String[] serverAddressList = config.getBrokerAddress().split("[;, ]");
		for(String serverAddress : serverAddressList){
			serverAddress = serverAddress.trim();
			if(serverAddress.isEmpty()) continue;
			registerServer(serverAddress);
		}
	}
	
	@Override
	public MessageInvoker selectForProducer(String topic) throws IOException {
		String serverAddress = serverSelector.selectForProducer(serverTable, topic); 
		if(serverAddress == null){
			throw new MqException("Missing broker for topic=" + topic);
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
		return broker.selectForProducer(topic);
	}

	@Override
	public MessageInvoker selectForConsumer(String topic) throws IOException {
		String serverAddress = serverSelector.selectForConsumer(serverTable, topic);
		if(serverAddress == null){
			throw new MqException("Missing broker for topic=" + topic);
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
		return broker.selectForConsumer(topic);
	}
	
	@Override
	public void releaseInvoker(MessageInvoker invoker) throws IOException {
		if(invoker instanceof JvmBroker){
			return;
		}
		if(!(invoker instanceof MessageClient)){
			return; 
		}
		
		MessageClient client = (MessageClient)invoker;
		String serverAddress = client.attr(Protocol.SERVER);
		if(serverAddress == null){
			throw new IllegalStateException("Can not find server in MessageClient attributes");
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
	    broker.releaseInvoker(invoker);
	}

	@Override
	public List<Broker> availableServerList() { 
		return new ArrayList<Broker>(brokerMap.values());
	}

	@Override
	public void setServerSelector(ServerSelector selector) {
		this.serverSelector = selector;
	}

	private Broker createBroker(String serverAddress) throws IOException{
		BrokerConfig config = this.config.clone();
		config.setBrokerAddress(serverAddress);
		Broker broker = new SingleBroker(config, eventDriver); 
		brokerMap.put(serverAddress, broker); 
		return broker;
	}
	
	@Override
	public void registerServer(final String serverAddress) throws IOException {
		final Broker broker;
		synchronized (brokerMap) {
			if(brokerMap.containsKey(serverAddress)){
				return;
			}  
			broker = createBroker(serverAddress);
		}    
		
		for(final ServerNotifyListener listener : listeners){
			eventDriver.getGroup().submit(new Runnable() { 
				@Override
				public void run() { 
					listener.onServerJoin(serverAddress, broker);
				}
			});
		}
	}

	@Override
	public void unregisterServer(final String serverAddress) throws IOException {
		final Broker broker;
		synchronized (brokerMap) { 
			broker = brokerMap.remove(serverAddress);
			if(broker == null) return;
			
			broker.close();
		}    
		
		for(final ServerNotifyListener listener : listeners){
			eventDriver.getGroup().submit(new Runnable() { 
				@Override
				public void run() { 
					listener.onServerLeave(serverAddress);
				}
			});
		}
	}

	@Override
	public void addServerListener(ServerNotifyListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeServerListener(ServerNotifyListener listener) {
		this.listeners.remove(listener);
	} 
	
	@Override
	public void close() throws IOException {
		synchronized (brokerMap) {
			for(Broker broker : brokerMap.values()){
				broker.close();
			}
			brokerMap.clear();
		} 
	}
}

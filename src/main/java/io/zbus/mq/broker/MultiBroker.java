package io.zbus.mq.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Broker;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.MqException;
import io.zbus.mq.Protocol;
import io.zbus.mq.net.MessageClient;

public class MultiBroker implements Broker {
	private Map<String, Broker> brokerMap = new ConcurrentHashMap<String, Broker>();
	private ServerSelector serverSelector = new DefaultServerSelector();
	private ServerTable serverTable = new ServerTable();
	
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
		if(!(invoker instanceof MessageClient)){
			return; //TODO
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
		this.serverSelector = selector; //TODO threadsafe
	}

	@Override
	public void registerServer(String serverAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterServer(String serverAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addServerListener(ServerNotifyListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeServerListener(ServerNotifyListener listener) {
		// TODO Auto-generated method stub
		
	} 
	
	@Override
	public void close() throws IOException { 
		
	}
}

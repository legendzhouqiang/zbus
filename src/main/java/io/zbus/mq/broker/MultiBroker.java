package io.zbus.mq.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Broker;
import io.zbus.mq.MessageInvoker;

public class MultiBroker implements Broker {
	private Map<String, Broker> brokerMap = new ConcurrentHashMap<String, Broker>();
	
	@Override
	public MessageInvoker selectForProducer(String topic) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageInvoker selectForConsumer(String topic) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void releaseInvoker(MessageInvoker invoker) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Broker> availableServerList() { 
		return new ArrayList<Broker>(brokerMap.values());
	}

	@Override
	public void onSelect(ServerSelector selector) {
		// TODO Auto-generated method stub
		
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

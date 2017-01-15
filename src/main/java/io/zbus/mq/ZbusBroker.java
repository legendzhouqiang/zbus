package io.zbus.mq;

import java.io.IOException;
import java.util.List;

import io.zbus.mq.broker.SingleBroker;
import io.zbus.mq.broker.TrackBroker;
 
public class ZbusBroker implements Broker{
	private Broker support; 
	public ZbusBroker() throws IOException { 
		this(new BrokerConfig());
	}
	
	public ZbusBroker(String brokerAddress) throws IOException {
		this(new BrokerConfig(brokerAddress));
	}
	 
	public ZbusBroker(BrokerConfig config) throws IOException { 
		String brokerAddress = config.getBrokerAddress();   
		brokerAddress = brokerAddress.trim();
		boolean ha = false;
		if(brokerAddress.startsWith("[")){
			if(brokerAddress.endsWith("]")){
				brokerAddress = brokerAddress.substring(1, brokerAddress.length()-1);
				ha = true;
			} else {
				throw new IllegalArgumentException(brokerAddress + " broker address invalid");
			}
		}  
		if(brokerAddress.contains(",") || brokerAddress.contains(" ") || brokerAddress.contains(";")){
			ha = true;
		} 
		config.setBrokerAddress(brokerAddress);
		if(ha){
			support = new TrackBroker(config);  
		} else {
			support = new SingleBroker(config);
		}
	}
	 
	@Override
	public MessageInvoker selectForProducer(String topic) throws IOException {
		return support.selectForProducer(topic);
	}
	
	@Override
	public MessageInvoker selectForConsumer(String topic) throws IOException {
		return support.selectForConsumer(topic);
	}

	@Override
	public void close() throws IOException {
		support.close();
	}
 
	@Override
	public void releaseInvoker(MessageInvoker client) throws IOException {
		support.releaseInvoker(client);
	}

	@Override
	public List<Broker> availableServerList() {
		return support.availableServerList();
	}

	@Override
	public void setServerSelector(ServerSelector selector) {
		support.setServerSelector(selector);
	}

	@Override
	public void registerServer(String serverAddress) {
		support.registerServer(serverAddress);
	}

	@Override
	public void unregisterServer(String serverAddress) {
		support.unregisterServer(serverAddress);
	}

	@Override
	public void addServerListener(ServerNotifyListener listener) {
		support.addServerListener(listener);
	}

	@Override
	public void removeServerListener(ServerNotifyListener listener) {
		support.removeServerListener(listener);
	}  
}

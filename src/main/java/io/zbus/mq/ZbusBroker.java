package io.zbus.mq;

import java.io.IOException;
import java.util.List;

import io.zbus.mq.broker.JvmBroker;
import io.zbus.mq.broker.TrackBroker;
import io.zbus.mq.server.MqServer;
 
public class ZbusBroker implements Broker{
	private Broker support;  
	
	public ZbusBroker(String brokerAddress) throws IOException {
		this(new BrokerConfig(brokerAddress));
	}
	 
	public ZbusBroker(BrokerConfig config) throws IOException { 
		String brokerAddress = config.getBrokerAddress();   
		if(brokerAddress != null){
			brokerAddress = brokerAddress.trim(); 
		}
		
		if(brokerAddress == null){
			MqServer server = (MqServer)config.getServerInJvm();
			if(server == null){
				throw new IllegalArgumentException("Try to initiate JvmBroker, but severInJvm(MqServer) not set");
			}
			support = new JvmBroker((MqServer)config.getServerInJvm());
			return;
		} 
		 
		brokerAddress = brokerAddress.trim();
		config.setBrokerAddress(brokerAddress); 
		support = new TrackBroker(config); 
		
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
	public void configServerSelector(ServerSelector selector) {
		support.configServerSelector(selector);
	}
 
	@Override
	public void addServer(String serverAddress) throws IOException{
		support.addServer(serverAddress);
	}
 
	@Override
	public void removeServer(String serverAddress) throws IOException {
		support.removeServer(serverAddress);
	}
 
	@Override
	public void addServerNotifyListener(ServerNotifyListener listener) {
		support.addServerNotifyListener(listener);
	}
 
	@Override
	public void removeServerNotifyListener(ServerNotifyListener listener) {
		support.removeServerNotifyListener(listener);
	}   
	
	@Override
	public String brokerAddress() { 
		return support.brokerAddress();
	}
}

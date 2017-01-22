package io.zbus.mq;

import java.io.IOException;
import java.util.List;

import io.zbus.mq.broker.JvmBroker;
import io.zbus.mq.broker.TrackBroker;
import io.zbus.mq.broker.SingleBroker;
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
		
		if(brokerAddress.split("[,; ]").length > 1){
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
	public void registerServer(String serverAddress) throws IOException{
		support.registerServer(serverAddress);
	}
 
	@Override
	public void unregisterServer(String serverAddress) throws IOException {
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

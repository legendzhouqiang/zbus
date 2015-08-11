package org.zstacks.zbus.rpc.inherit;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;


public class InheritedClient { 
	public static void main(String[] args) throws Exception{  
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		
		A a = proxy.getService(A.class, "mq=InheritTest");
		System.out.println(a.getString());
		
		broker.close();
	} 
	
}

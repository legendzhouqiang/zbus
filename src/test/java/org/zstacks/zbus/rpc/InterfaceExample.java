package org.zstacks.zbus.rpc;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;

public class InterfaceExample { 
	public static void test(RpcProxy proxy) throws Exception{
		
		
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc");
 
		
		Object[] objects = hello.getUsers();
		for(Object obj : objects){
			System.out.println(obj);
		}
	}
	

	public static void main(String[] args) throws Exception { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		test(proxy);
		
		broker.close();
	}
}

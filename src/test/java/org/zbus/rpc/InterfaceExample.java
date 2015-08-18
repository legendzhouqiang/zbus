package org.zbus.rpc;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.rpc.RpcProxy;

public class InterfaceExample { 
	public static void test(RpcProxy proxy) throws Exception{
		
		
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc");
 
		
		Object[] objects = hello.getUsers();
		for(Object obj : objects){
			System.out.println(obj);
		}
	}
	

	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		test(proxy);
		
		broker.close();
	}
}

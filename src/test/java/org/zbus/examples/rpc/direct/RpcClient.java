package org.zbus.examples.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.Interface;
import org.zbus.rpc.RpcFactory;

public class RpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(brokerConfig); 
		
		RpcFactory factory = new RpcFactory(broker); //directly using broker as invoker 
		Interface hello = factory.getService(Interface.class);
		
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close();
	}
}

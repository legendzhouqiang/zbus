package io.zbus.examples.rpc;

import io.zbus.examples.rpc.api.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcFactory;

public class RpcInvokerProxyClass {

	public static void main(String[] args) throws Exception {  
		Broker broker = new Broker();  
		broker.addServer("localhost:15555", "ssl/zbus.crt");
		
		RpcConfig config = new RpcConfig(broker);  
		config.setTopic("MyRpc"); 
		
		InterfaceExample api = RpcFactory.getService(InterfaceExample.class, config);
		RpcTestCases.testDynamicProxy(api);
		
		broker.close(); 
	}

}

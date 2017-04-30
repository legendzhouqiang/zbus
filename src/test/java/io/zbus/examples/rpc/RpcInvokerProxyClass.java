package io.zbus.examples.rpc;

import io.zbus.examples.rpc.api.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcFactory;

public class RpcInvokerProxyClass {

	public static void main(String[] args) throws Exception {  
		Broker broker = new Broker("conf/broker.xml");
		//Broker broker = new Broker(); 
		//broker.addTracker("localhost:15555", "ssl/zbus.crt");
		
		RpcConfig config = new RpcConfig(broker);  
		config.setTopic("MyRpc"); 
		
		InterfaceExample api = RpcFactory.getService(InterfaceExample.class, config);
		for(int i=0;i<10000;i++){
			try{
				RpcTestCases.testDynamicProxy(api);
			} catch (Exception e) {
				e.printStackTrace(); 
			} 
			Thread.sleep(2000);
		}
		
		broker.close(); 
	}

}

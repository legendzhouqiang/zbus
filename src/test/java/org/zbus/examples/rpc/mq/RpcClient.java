package org.zbus.examples.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClient { 
	
	public static void main(String[] args) throws Exception {  
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555"); 
		
		Broker broker = new ZbusBroker(config); 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		
		//use RpcFactory to generate dynamic implementation via zbus
		RpcFactory factory = new RpcFactory(invoker);   
		InterfaceExample hello = factory.getService(InterfaceExample.class);
		
		for(int i=0;i<1000;i++)
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close(); 
	}  
}

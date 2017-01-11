package io.zbus.examples.rpc;

import io.zbus.examples.rpc.appdomain.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.mq.broker.BrokerConfig;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message.MessageInvoker;
import io.zbus.rpc.RpcFactory;
import io.zbus.rpc.mq.MqInvoker;

public class RpcClient { 
	
	public static void main(String[] args) throws Exception {  
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555"); 
		
		Broker broker = new ZbusBroker(config); 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		
		//use RpcFactory to generate dynamic implementation via zbus 
		RpcFactory factory = new RpcFactory(invoker);   
		InterfaceExample hello = factory.getService(InterfaceExample.class);
		 
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close(); 
	}  
}

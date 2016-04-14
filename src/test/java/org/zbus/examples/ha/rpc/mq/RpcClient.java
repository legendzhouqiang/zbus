package org.zbus.examples.ha.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClient { 
	
	public static void main(String[] args) throws Exception {   
		
		Broker broker = new ZbusBroker("[127.0.0.1:16666;127.0.0.1:16667]"); 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		

		RpcFactory factory = new RpcFactory(invoker);   
		InterfaceExample hello = factory.getService(InterfaceExample.class); 
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close(); 
	}  
}

package io.zbus.examples.ha.rpc;

import io.zbus.examples.rpc.RpcCases;
import io.zbus.examples.rpc.appdomain.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message.MessageInvoker;
import io.zbus.rpc.RpcFactory;
import io.zbus.rpc.mq.MqInvoker;

public class RpcClient { 
	
	public static void main(String[] args) throws Exception {   
		
		//If single track server is in use, !!!DO keep the last ';' in the address!!!
		Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667"); 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		

		RpcFactory factory = new RpcFactory(invoker);   
		InterfaceExample hello = factory.getService(InterfaceExample.class); 
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close(); 
	}  
}

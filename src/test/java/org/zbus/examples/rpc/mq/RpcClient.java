package org.zbus.examples.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.Interface;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClient { 
	
	public static void main(String[] args) throws Exception {  
		Broker broker = new SingleBroker(); 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		
		//use RpcFactory to generate dynamic implementation via zbus
		RpcFactory factory = new RpcFactory(invoker);   
		Interface hello = factory.getService(Interface.class);
		
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close();
	}  
}

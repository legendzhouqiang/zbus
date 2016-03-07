package org.zbus.examples.jvm;

import org.zbus.broker.Broker;
import org.zbus.broker.JvmBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.mq.MqInvoker;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class JvmRpcExample {

	public static void main(String[] args) throws Exception {  
		Broker broker = new JvmBroker(); 
 
		
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		 
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(2); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor);   
		
		Service svc = new Service(config);
		svc.start();  
		
		
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");  
		
		//use RpcFactory to generate dynamic implementation via zbus
		RpcFactory factory = new RpcFactory(invoker);   
		InterfaceExample hello = factory.getService(InterfaceExample.class);
		
		RpcCases.testDynamicProxy(hello); //test cases
		
		
		svc.close();
		broker.close(); 
		
		System.out.println("==done== waiting fastjson to be destroyed....(improve fastjson)");
	}

}

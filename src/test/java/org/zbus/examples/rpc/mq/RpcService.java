package org.zbus.examples.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{     
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		
		Broker broker = new SingleBroker(); //use BrokerConfig with non-defaults
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(2); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor);   
		
		Service svc = new Service(config);
		svc.start();  
	} 
}

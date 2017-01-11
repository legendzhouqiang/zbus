package io.zbus.examples.ha.rpc;

import java.io.IOException;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;
import io.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.ConsumerServiceConfig;
import io.zbus.rpc.RpcProcessor;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{     
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		
		Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667");
		//Broker broker = new ZbusBroker("127.0.0.1:15555");

		ConsumerServiceConfig config = new ConsumerServiceConfig();
		config.setConsumerCount(2); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor); 
		config.setVerbose(true);
		
		ConsumerService svc = new ConsumerService(config);
		svc.start();  
	}
}

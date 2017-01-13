package io.zbus.examples.rpc;

import java.io.IOException;

import io.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import io.zbus.mq.Broker;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.ConsumerServiceConfig;
import io.zbus.mq.ZbusBroker;
import io.zbus.rpc.RpcProcessor;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{     
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new InterfaceExampleImpl());  
		
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		ConsumerServiceConfig config = new ConsumerServiceConfig(); 
		config.setTopic("MyRpc");  
		config.setBroker(broker);    
		config.setMessageProcessor(processor);  
		config.setThreadPoolSize(100);   
		
		ConsumerService svc = new ConsumerService(config);
		svc.start();  
	}
}

package io.zbus.examples.rpc;

import io.zbus.examples.rpc.api.InterfaceExampleImpl;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.rpc.RpcProcessor;

public class RpcProcessorExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(new InterfaceExampleImpl());  
		
		
		Broker broker = new Broker("localhost:15555");   
		ConsumerConfig config = new ConsumerConfig();
		config.setBroker(broker);
		config.setTopic("MyRpc");
		config.setMessageHandler(processor);   
		
		Consumer consumer = new Consumer(config); 
		
		consumer.start(); 
	} 
}

package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.BaseExtImpl;
import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;

public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		//With RpcProcessor, we only care about the moudles and bussiness logic, nothing related to zbus
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(InterfaceExampleImpl.class); //By default interface full name, empty are used as module name
		//processor.addModule(module, services); //You can define module name, it is optional
		processor.addModule(BaseExtImpl.class);
		
		
		//The following is same as a simple Consumer setup process
		String topic = "MyRpc";
		String token = "MyRpc_Service"; 
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackerList("localhost:15555");
		brokerConfig.setToken(token); 
		Broker broker = new Broker(brokerConfig);  
		
		ConsumerConfig config = new ConsumerConfig(broker); 
		config.setTopic(topic);
		config.setToken(token); //access control
		config.setTopicMask(Protocol.MASK_MEMORY); //RPC, choose memory queue to boost speed
		config.setMessageHandler(processor);   
		
		Consumer consumer = new Consumer(config); 
		
		consumer.start(); 
	} 
}

package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.BaseExtImpl;
import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.examples.rpc.biz.generic.GenericMethodImpl;
import io.zbus.examples.rpc.biz.inheritance.SubService1;
import io.zbus.examples.rpc.biz.inheritance.SubService2;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.ServerAddress;

public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		// With RpcProcessor, we only care about the moudles and bussiness  logic, nothing related to zbus
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(InterfaceExampleImpl.class); // By default interface full name, empty are used as  module name
		// processor.addModule(module, services); //You can define module name,  it is optional
		processor.addModule(BaseExtImpl.class);
		processor.addModule(GenericMethodImpl.class);
		processor.addModule(SubService1.class);
		processor.addModule(SubService2.class);

		
		
		// The following is same as a simple Consumer setup process 
		boolean enableSsl = false;
		ServerAddress trackerAddress = new ServerAddress("localhost:15555");
		if(enableSsl){
			trackerAddress.setCertFile("ssl/zbus.crt");
			trackerAddress.setSslEnabled(true);	
		} 
		trackerAddress.setToken("myrpc_service"); // Token for tracker,

		Broker broker = new Broker();
		broker.addTracker(trackerAddress); //Add more trackers if available, for HA

		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyRpc");
		config.setToken("myrpc_service"); // access control
		config.setTopicMask(Protocol.MASK_MEMORY); // RPC, choose memory queue  to boost speed
		config.setMessageHandler(processor);

		Consumer consumer = new Consumer(config);

		consumer.start();
	}
}

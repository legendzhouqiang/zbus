package io.zbus.examples.rpc.direct;

import io.zbus.examples.rpc.biz.BaseExtImpl;
import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.examples.rpc.biz.generic.GenericMethodImpl;
import io.zbus.examples.rpc.biz.inheritance.SubService1;
import io.zbus.examples.rpc.biz.inheritance.SubService2;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;
import io.zbus.rpc.RpcProcessor;

public class RpcServiceInprocServer1 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		 
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(InterfaceExampleImpl.class);   
		processor.addModule(BaseExtImpl.class);
		processor.addModule(GenericMethodImpl.class);
		processor.addModule(SubService1.class);
		processor.addModule(SubService2.class);
		
		
		//Work along with MqServer in single NODE, in zbus we call it direct rpc 
		MqServerConfig serverConfig = new MqServerConfig();
		serverConfig.setServerPort(15555);
		MqServer server = new MqServer(serverConfig);  //Start zbus internally
		server.start();
		
		Broker broker = new Broker(server);     
		ConsumerConfig config = new ConsumerConfig(broker); 
		config.setTopic("MyRpc");
		config.setTopicMask(Protocol.MASK_MEMORY); //RPC, choose memory queue to boost speed
		config.setMessageHandler(processor);    
		
		Consumer consumer = new Consumer(config);  
		consumer.start(); 
	} 
}

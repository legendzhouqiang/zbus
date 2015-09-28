package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class MqRpcServiceMultiBroker {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		RpcProcessor processor = new RpcProcessor();
		// 增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl());
 
		
		BrokerConfig brokerConfig1 = new BrokerConfig();
		brokerConfig1.setServerAddress("127.0.0.1:15555");
		Broker broker1 = new SingleBroker(brokerConfig1);
		
		BrokerConfig brokerConfig2 = new BrokerConfig();
		brokerConfig2.setServerAddress("127.0.0.1:15556");
		Broker broker2 = new SingleBroker(brokerConfig2);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(10); 
		config.setMq("MyRpc");  
		//同时注册到多条zbus总线上
		config.setBrokers(new Broker[]{broker1, broker2});
		config.setMessageProcessor(processor);
		
		Service svc = new Service(config);
		svc.start();  
	} 
}

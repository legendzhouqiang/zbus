package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class MqRpcServiceHA {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		RpcProcessor processor = new RpcProcessor();
		// 增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl());
 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666;127.0.01:16667");
		Broker broker = new HaBroker(brokerConfig);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(20); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);   
		config.setMessageProcessor(processor);
		
		Service svc = new Service(config);
		svc.start();  
	} 
}

package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class MqRpcService {
	public static void main(String[] args) throws IOException{   
		
		RpcProcessor processor = new RpcProcessor(); 
		//增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl()); 
		
		//配置Broker
		BrokerConfig brokerCfg = new BrokerConfig();
		brokerCfg.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerCfg);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(20); 
		config.setMq("MyRpc"); 
		config.setBroker(broker);   
		config.setMessageProcessor(processor);
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();  
	} 
}

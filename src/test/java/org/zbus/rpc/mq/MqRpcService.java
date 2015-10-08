package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class MqRpcService {
	public static void main(String[] args) throws IOException{   
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 32); 
		final String mq = ConfigKit.option(args, "-mq", "MyRpc");
		
		RpcProcessor processor = new RpcProcessor(); 
		//增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl()); 
		
		//配置Broker
		BrokerConfig brokerCfg = new BrokerConfig();
		brokerCfg.setServerAddress(serverAddress); 
		brokerCfg.setMaxTotal(threadCount);
		brokerCfg.setMinIdle(threadCount);
		
		Broker broker = new SingleBroker(brokerCfg);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(threadCount); 
		config.setMq(mq); 
		config.setBroker(broker);    
		config.setMessageProcessor(processor); 
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();  
	} 
}

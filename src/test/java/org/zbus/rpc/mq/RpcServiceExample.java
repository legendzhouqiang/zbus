package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class RpcServiceExample {
	public static void main(String[] args) throws IOException{  
		String address = ConfigKit.option(args, "-b", "127.0.0.1:15555"); 
		int conusmerCount = ConfigKit.option(args, "-consumer", 20); //消费者数目 
		String service = ConfigKit.option(args, "-service", "MyRpc"); 
		
		//配置Broker
		BrokerConfig brokerCfg = new BrokerConfig();
		brokerCfg.setBrokerAddress(address);
		Broker broker = new SingleBroker(brokerCfg);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(conusmerCount); 
		config.setMq(service); 
		config.setBroker(broker); 
		
		RpcProcessor processor = new RpcProcessor(); 
		//增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl());   
				
		//处理逻辑
		config.setMessageProcessor(processor);
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();  
	} 
}

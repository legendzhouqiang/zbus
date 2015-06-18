package org.zstacks.zbus.rpc;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcServiceHandler;
import org.zstacks.zbus.client.service.Service;
import org.zstacks.zbus.client.service.ServiceConfig;
import org.zstacks.zbus.rpc.biz.InterfaceImpl;
import org.zstacks.znet.Helper;

public class RpcServiceExample {
	public static void main(String[] args) throws IOException{  
		String address = Helper.option(args, "-b", "127.0.0.1:15555"); 
		int conusmerCount = Helper.option(args, "-consumer", 2); //消费者数目
		int threadCount = Helper.option(args, "-thread", 20);  //业务执行线程数目
		String service = Helper.option(args, "-service", "MyRpc");
		boolean verbose = Helper.option(args, "-verbose", true);
		
		//配置Broker
		SingleBrokerConfig brokerCfg = new SingleBrokerConfig();
		brokerCfg.setBrokerAddress(address);
		Broker broker = new SingleBroker(brokerCfg);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(conusmerCount);
		config.setThreadCount(threadCount); 
		config.setMq(service); 
		config.setBroker(broker);
		config.setVerbose(verbose);
		
		RpcServiceHandler handler = new RpcServiceHandler(); 
		//增加模块，模块名在调用时需要指定
		handler.addModule(new InterfaceImpl());   
				
		//处理逻辑
		config.setServiceHandler(handler);
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();  
	} 
}

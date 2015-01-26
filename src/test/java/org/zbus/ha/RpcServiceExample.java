package org.zbus.ha;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Service;
import org.zbus.client.ServiceConfig;
import org.zbus.client.broker.HaBroker;
import org.zbus.client.broker.HaBrokerConfig;
import org.zbus.client.rpc.RpcServiceHandler;
import org.zbus.rpc.biz.ServiceImpl;
import org.zbus.rpc.biz.ServiceInterface;

public class RpcServiceExample {
	public static void main(String[] args) throws IOException{   
		ServiceConfig config = new ServiceConfig();
		config.setThreadCount(4); 
		config.setServiceName("MyRpc");
		//配置Broker
		HaBrokerConfig brokerConfig = new HaBrokerConfig();
		brokerConfig.setTrackAddrList("127.0.0.1:16666:127.0.0.1:16667");
		Broker broker = new HaBroker(brokerConfig);
		config.setBroker(broker);
		
		RpcServiceHandler handler = new RpcServiceHandler(); 
		//增加模块，模块名在调用时需要指定
		handler.addModule(ServiceInterface.class, new ServiceImpl());  
	    //handler.addModule("ServiceInterface", new ServiceImpl()); //可以指定模块名
				
		//处理逻辑
		config.setServiceHandler(handler);
		
		Service svc = new Service(config);
		svc.start();  
	} 
}

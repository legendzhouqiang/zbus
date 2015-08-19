package org.zbus;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.pool.Pool;
import org.zbus.pool.impl.DefaultPoolFactory;
import org.zbus.rpc.service.Service;
import org.zbus.rpc.service.ServiceConfig;
import org.zbus.rpc.service.ServiceHandler;

public class ServiceExample {
	
	public static void main(String[] args) throws IOException, Exception{  
		String address = ConfigKit.option(args, "-b", "127.0.0.1:15555"); 
		int consumerCount = ConfigKit.option(args, "-c", 20);
		String service = ConfigKit.option(args, "-s", "MyService");
		
		Pool.setPoolFactory(new DefaultPoolFactory());
		
		//配置Broker
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(address); 
		Broker broker = new SingleBroker(brokerConfig);
		
		ServiceConfig config = new ServiceConfig(); 
		config.setMq(service);
		config.setBroker(broker);
		config.setConsumerCount(consumerCount);
		
		//处理逻辑
		config.setServiceHandler(new ServiceHandler() { 
			public Message handleRequest(Message request) { 
				//System.out.println(request);
				Message result = new Message();
				result.setResponseStatus("200");
				result.setBody("Server time: "+System.currentTimeMillis());	
				return result;
			}
		});
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();
	} 
	
}

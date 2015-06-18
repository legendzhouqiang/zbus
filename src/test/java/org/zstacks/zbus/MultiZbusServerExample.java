package org.zstacks.zbus;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.service.Service;
import org.zstacks.zbus.client.service.ServiceConfig;
import org.zstacks.zbus.client.service.ServiceHandler;
import org.zstacks.znet.Helper;
import org.zstacks.znet.Message;

public class MultiZbusServerExample {
	
	public static void main(String[] args) throws IOException, Exception{  
		String address = Helper.option(args, "-b", "127.0.0.1:15555"); 
		int threadCount = Helper.option(args, "-c", 2);
		String service = Helper.option(args, "-s", "MyService");
		
		//配置Broker
		SingleBrokerConfig brokerConfig = new SingleBrokerConfig();
		brokerConfig.setBrokerAddress(address);
		brokerConfig.setMaxTotal(threadCount); 
		
		SingleBrokerConfig brokerConfig2 = new SingleBrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15556");
		brokerConfig.setMaxTotal(threadCount);
		
		//两个Broker
		Broker broker1 = new SingleBroker(brokerConfig);
		Broker broker2 = new SingleBroker(brokerConfig2);
		
		ServiceConfig config = new ServiceConfig(broker1, broker2);
		config.setThreadCount(threadCount); 
		config.setMq(service); 
		
		//处理逻辑
		config.setServiceHandler(new ServiceHandler() { 
			public Message handleRequest(Message request) { 
				//System.out.println(request);
				Message result = new Message();
				result.setStatus("200");
				result.setBody("Server time: "+System.currentTimeMillis());	
				return result;
			}
		});
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();
	} 
	
}
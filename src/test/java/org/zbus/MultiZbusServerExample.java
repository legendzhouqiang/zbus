package org.zbus;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.rpc.service.Service;
import org.zbus.rpc.service.ServiceConfig;
import org.zbus.rpc.service.ServiceHandler;

public class MultiZbusServerExample {
	
	public static void main(String[] args) throws IOException, Exception{  
		String address = ConfigKit.option(args, "-b", "127.0.0.1:15555"); 
		int threadCount = ConfigKit.option(args, "-c", 2);
		String service = ConfigKit.option(args, "-s", "MyService");
		
		//配置Broker
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(address);
		brokerConfig.setMaxTotal(threadCount); 
		
		BrokerConfig brokerConfig2 = new BrokerConfig();
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

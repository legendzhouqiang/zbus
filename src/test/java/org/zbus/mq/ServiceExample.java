package org.zbus.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class ServiceExample {
	
	public static void main(String[] args) throws IOException, Exception{  
		//配置Broker
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555"); 
		Broker broker = new SingleBroker(brokerConfig);
		
		ServiceConfig config = new ServiceConfig(); 
		config.setMq("MyService");
		config.setBroker(broker);
		config.setConsumerCount(2);
		
		//处理逻辑
		config.setMessageProcessor(new MessageProcessor() { 
			@Override
			public Message process(Message request) {  
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

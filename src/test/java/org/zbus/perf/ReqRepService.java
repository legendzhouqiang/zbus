package org.zbus.perf;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class ReqRepService {
	public static void main(String[] args) throws IOException{    
		//配置Broker
		BrokerConfig brokerCfg = new BrokerConfig();
		brokerCfg.setServerAddress("127.0.0.1:15555"); 
		Broker broker = new SingleBroker(brokerCfg);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(100); 
		config.setMq("ReqRep"); 
		config.setBroker(broker);   
		config.setMessageProcessor(new MessageProcessor() { 
			@Override
			public Message process(Message request) { 
				return request;
			}
		});
		
		@SuppressWarnings("resource")
		Service svc = new Service(config);
		svc.start();  
	} 
}

package org.zbus.examples.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class ConsumerMultiBroker {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException { 
		BrokerConfig brokerConfig1 = new BrokerConfig();
		brokerConfig1.setServerAddress("127.0.0.1:15555");
		Broker broker1 = new SingleBroker(brokerConfig1);
		
		BrokerConfig brokerConfig2 = new BrokerConfig();
		brokerConfig2.setServerAddress("127.0.0.1:15556");
		Broker broker2 = new SingleBroker(brokerConfig2);
		
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(10); 
		config.setMq("MyRpc");  
		//同时注册到多条zbus总线上
		config.setBrokers(new Broker[]{broker1, broker2});
		
		config.setMessageHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				
			}
		}); 
		
		Service svc = new Service(config);
		svc.start();  
	} 
}

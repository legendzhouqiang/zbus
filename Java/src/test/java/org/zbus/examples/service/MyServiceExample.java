package org.zbus.examples.service;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class MyServiceExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		ServiceConfig config = new ServiceConfig();
		config.setBroker(broker);
		config.setMq("MyService"); 
		
		config.setMessageProcessor(new MessageProcessor() { 
			@Override
			public Message process(Message request) { 
				System.out.println(request); 
				return request;
			}
		});
		
		Service svc = new Service(config);
		svc.start();  
	}

}

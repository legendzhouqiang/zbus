package io.zbus.examples.auth;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.broker.ZbusBroker;

public class ConsumerAuthExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setMq("MyMQ_Auth");
		
		
		//appid + token
		config.setAppid("appid");
		config.setToken("token");  
		
		Consumer consumer = new Consumer(config);   
		consumer.declareQueue();
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});    
	}
}

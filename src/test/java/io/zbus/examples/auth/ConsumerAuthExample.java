package io.zbus.examples.auth;

import java.io.IOException;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.net.http.Message;

public class ConsumerAuthExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		
		ConsumerConfig config = new ConsumerConfig();
		config.setBroker(broker);
		config.setMq("MyMQ_Auth");
		
		
		//appid + token
		config.setAppid("appid");
		config.setToken("token");  
		
		Consumer consumer = new Consumer(config);   
		consumer.declareMQ();
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});    
	}
}

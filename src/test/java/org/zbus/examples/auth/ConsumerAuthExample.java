package org.zbus.examples.auth;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.net.http.Message;

public class ConsumerAuthExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		
		MqConfig config = new MqConfig();
		config.setMq("MyMQ_Auth");
		config.setAccessToken("MyMQ_Token"); //token to access this queue
		config.setBroker(broker);
		
		Consumer consumer = new Consumer(config);  
		 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});    
	}
}

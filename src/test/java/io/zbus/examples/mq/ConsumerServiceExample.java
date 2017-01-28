package io.zbus.examples.mq;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.ConsumerServiceConfig;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;

public class ConsumerServiceExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Broker broker = new ZbusBroker("127.0.0.1:15555");    
		
		ConsumerServiceConfig config = new ConsumerServiceConfig();
		config.setBroker(broker);
		config.setTopic("MyMQ");   
		config.setConsumerCount(4);
		
		config.setConsumerHandler(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {
				System.out.println(msg);
			}
		});
		
		ConsumerService service = new ConsumerService(config);
		service.start(); 
	}
}

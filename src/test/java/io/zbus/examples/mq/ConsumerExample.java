package io.zbus.examples.mq;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class ConsumerExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.declareQueue();
		 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});    
	}
}

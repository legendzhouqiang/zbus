package io.zbus.examples.mq;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;

public class ConsumerCloseClear { 
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.declareTopic();
		
		Message message = consumer.take(); 
		System.out.println(message);
		 
		Thread.sleep(1000); 
		consumer.close();
		
		broker.close();
		System.out.println("closed");
	}
}

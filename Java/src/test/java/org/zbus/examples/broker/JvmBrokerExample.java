package org.zbus.examples.broker;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.JvmBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class JvmBrokerExample {

	public static void main(String[] args) throws Exception {  
		Broker broker = new JvmBroker();
		
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		Message message = new Message();
		message.setBody("test body");
		producer.sendAsync(message);
		
		Thread.sleep(100);
		System.out.println("destroy consumer");
		consumer.close();
		System.out.println("destroy broker");
		broker.close(); 
		System.out.println("destroyed environment");
		
	} 
	
}

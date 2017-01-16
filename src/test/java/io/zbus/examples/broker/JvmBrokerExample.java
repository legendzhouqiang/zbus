package io.zbus.examples.broker;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.broker.JvmBroker;
import io.zbus.mq.server.MqServer;

public class JvmBrokerExample {

	public static void main(String[] args) throws Exception {  
		MqServer server = new MqServer();
		
		Broker broker = new JvmBroker(server);
		
		ConsumerService service = new ConsumerService(broker, "MyMQ");  
		service.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		Message message = new Message();
		message.setBody("test body");
		producer.publishAsync(message);
		
		Thread.sleep(100);
		System.out.println("destroy consumer");
		service.close();
		System.out.println("destroy broker");
		broker.close(); 
		System.out.println("destroyed environment");
		
	} 
	
}

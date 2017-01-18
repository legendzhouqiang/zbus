package io.zbus.examples.broker;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ZbusBroker;
import io.zbus.mq.server.MqServer;

public class JvmBrokerExample {

	public static void main(String[] args) throws Exception {  
		MqServer server = new MqServer();
		BrokerConfig config = new BrokerConfig();
		config.setServerInJvm(server);
		
		Broker broker = new ZbusBroker(config);
		
		ConsumerService service = new ConsumerService(broker, "MyMQ2");  
		service.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ2");
		Message message = new Message();
		message.setBody("test body");
		producer.publish(message);
		
		Thread.sleep(100);
		System.out.println("destroy consumer");
		service.close();
		System.out.println("destroy broker");
		broker.close(); 
		 
		System.out.println("destroye server");
		server.close();
		
	} 
	
}

package io.zbus.mq;

import java.io.IOException;

import io.zbus.mq.Consumer;
import io.zbus.mq.broker.ZbusBroker;

public class ConsumerTest {  
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});   
		 
		consumer.close();
		System.out.println("close broker");
		broker.close();
	}
}

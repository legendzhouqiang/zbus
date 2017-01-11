package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class Sub4 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		ConsumerConfig config = new ConsumerConfig();
		config.setBroker(broker);
		config.setMq("MyMQ");
		config.setConsumeGroup("Group4");
		
		Consumer c = new Consumer(config);    
		c.declareMQ();
		
		c.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {   
				System.out.println(msg); 
			}
		});    
	} 
}

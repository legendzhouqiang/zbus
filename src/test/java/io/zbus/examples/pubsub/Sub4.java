package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.mq.MqConfig;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class Sub4 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setMq("MyMQ");
		config.setConsumeGroup("Group4");
		
		Consumer c = new Consumer(config);    
		c.declareQueue();
		
		c.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {   
				System.out.println(msg); 
			}
		});    
	} 
}

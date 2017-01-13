package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.Message;
import io.zbus.mq.broker.ZbusBroker;

public class Sub1 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		final Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		
		Consumer c = new Consumer(broker, "MyMQ");  
		c.setConsumeGroup("Group1"); //different groups consumes the same MQ data	 
		c.declareQueue();
		
		c.start(new ConsumerHandler() {
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {
				System.out.println(msg);
			}
		});
	}
}

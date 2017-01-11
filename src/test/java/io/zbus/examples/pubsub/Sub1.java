package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.net.http.Message;

public class Sub1 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		final Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		
		Consumer c = new Consumer(broker, "MyMQ");  
		c.setConsumeGroup("Group1"); //different groups consumes the same MQ data	 
		c.declareMQ();
		
		c.start(new ConsumerHandler() {
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {
				System.out.println(msg);
			}
		});
	}
}

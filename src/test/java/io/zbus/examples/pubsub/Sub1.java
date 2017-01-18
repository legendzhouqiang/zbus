package io.zbus.examples.pubsub;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;

public class Sub1 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		final Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		
		Consumer c = new Consumer(broker, "MyMQ");  
		c.setConsumerGroup("Group1"); //different groups consumes the same MQ data	 
		c.declareTopic();
		
		while(true){
			Message message = c.take();
			System.out.println(message);
		} 
	}
}

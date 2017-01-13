package io.zbus.examples.pubsub;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;

public class Sub2 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Consumer c = new Consumer(broker, "MyMQ");   
		ConsumeGroup group = new ConsumeGroup("Group2"); 
		c.setConsumeGroup(group);  
		
		c.declareTopic();
		
		while(true){
			Message message = c.take();
			System.out.println(message);
		} 
	} 
}

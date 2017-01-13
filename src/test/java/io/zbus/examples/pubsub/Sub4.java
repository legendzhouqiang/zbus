package io.zbus.examples.pubsub;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.ZbusBroker;

public class Sub4 {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setTopic("MyMQ");
		config.setConsumeGroup("Group4");
		
		Consumer c = new Consumer(config);    
		c.declareTopic();
		
		while(true){
			Message message = c.take();
			System.out.println(message);
		} 
	} 
}

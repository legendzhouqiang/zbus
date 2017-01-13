package io.zbus.examples.pubsub;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.ZbusBroker;

public class Sub_FilterTag_Star {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setTopic("MyMQ");
		ConsumeGroup group = new ConsumeGroup();
		group.setGroupName("Group5");
		group.setFilterTag("abc.*");
		
		config.setConsumeGroup(group);  
		
		Consumer c = new Consumer(config);    
		c.declareTopic();
		
		c.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {   
				System.out.println(msg); 
			}
		});    
	} 
}

package org.zbus.examples.pubsub;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.ConsumeGroup;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.ConsumerConfig;
import org.zbus.net.http.Message;

public class Sub_FilterTag_Sharp {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		
		
		
		ConsumerConfig config = new ConsumerConfig();
		config.setBroker(broker);
		config.setMq("MyMQ"); 
		
		ConsumeGroup group = new ConsumeGroup();
		group.setGroupName("Group6");
		group.setFilterTag("abc.#"); //abc.xx, abc.yy.
		
		config.setConsumeGroup(group);  
		
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

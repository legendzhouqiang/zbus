package io.zbus.examples.pubsub;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.ZbusBroker;

public class Sub_FilterTag_AnySharp {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setTopic("MyMQ"); 
		
		ConsumeGroup group = new ConsumeGroup();
		group.setGroupName("Group6");
		group.setFilterTag("abc.#"); //abc.xx, abc.yy.
		
		config.setConsumeGroup(group);  
		
		Consumer c = new Consumer(config);    
		c.declareTopic();
		
		while(true){
			Message message = c.take();
			System.out.println(message);
		} 
	} 
}

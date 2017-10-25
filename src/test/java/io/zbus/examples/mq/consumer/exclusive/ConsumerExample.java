package io.zbus.examples.mq.consumer.exclusive;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.mq.Protocol;

public class ConsumerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");   
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic"); 
		
		ConsumeGroup consumerGroup = new ConsumeGroup(); 
		consumerGroup.setGroupName("Group1");
		consumerGroup.setMask(Protocol.MASK_EXCLUSIVE | Protocol.MASK_DELETE_ON_EXIT); 
		
		config.setConsumeGroup(consumerGroup); 
		
		config.setMessageHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);
			}
		});
		
		Consumer consumer = new Consumer(config);
		consumer.start(); 
	} 
}

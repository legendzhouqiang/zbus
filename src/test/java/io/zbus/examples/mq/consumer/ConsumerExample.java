package io.zbus.examples.mq.consumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;
import io.zbus.mq.Protocol;

public class ConsumerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");   
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic"); //ConsumeGroup default to same as topic 
		ConsumeGroup group = new ConsumeGroup(); 
		group.setMask(Protocol.MASK_ACK_REQUIRED); 
		group.setAckWindow(10);
		group.setAckTimeout(TimeUnit.SECONDS, 10);
		
		config.setConsumeGroup(group); 
		config.setMessageHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);    
				 
				client.ack(msg);
			}
		});
		
		Consumer consumer = new Consumer(config);
		consumer.start(); 
	} 
}

package io.zbus.examples.auth;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.ZbusBroker;

public class ConsumerAuthExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		
		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setTopic("MyMQ_Auth"); 
		
		//appid + token
		config.setAppid("appid");
		config.setToken("token");  
		
		Consumer consumer = new Consumer(config);   
		consumer.declareTopic();
		while(true){
			Message message = consumer.take();
			System.out.println(message);
		}   
	}
}

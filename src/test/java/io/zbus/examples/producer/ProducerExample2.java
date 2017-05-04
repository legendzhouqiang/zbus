package io.zbus.examples.producer;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig; 

public class ProducerExample2 {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker();
		broker.addServer("localhost:15555"); 
		
		ProducerConfig config = new ProducerConfig(broker);   
		
		Producer p = new Producer(config);
		p.declareTopic("hong"); 
		 
		Message msg = new Message();
		msg.setTopic("hong");
		msg.setTag("group1.xxx");
		msg.setBody("hello "+System.currentTimeMillis()); 
		
		Message res = p.publish(msg);
		System.out.println(res);  
		 
		 
		broker.close();
	}

}

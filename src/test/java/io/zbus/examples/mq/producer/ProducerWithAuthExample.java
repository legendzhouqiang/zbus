package io.zbus.examples.mq.producer;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer; 

public class ProducerWithAuthExample { 
	public static void main(String[] args) throws Exception { 
		String token = "MyTopic_token";
		Broker broker = new Broker("localhost:15555", token); 
		  
		Producer p = new Producer(broker);
		p.declareTopic("MyTopic"); 
		 
		Message msg = new Message();
		msg.setToken(token);
		msg.setTopic("MyTopic");
		//msg.setTag("oo.account.pp");
		msg.setBody("hello " + System.currentTimeMillis()); 
		
		Message res = p.publish(msg);
		System.out.println(res);   
		 
		broker.close();
	} 
}

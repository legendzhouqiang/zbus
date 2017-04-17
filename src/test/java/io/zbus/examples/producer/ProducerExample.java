package io.zbus.examples.producer;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig; 

public class ProducerExample {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555"); 
		ProducerConfig config = new ProducerConfig(broker);   
		
		Producer p = new Producer(config);
		p.declareTopic("Topic1"); 
		
		for(int i=0; i<10000; i++){
		Message msg = new Message();
		msg.setTopic("Topic1");
		msg.setBody("hello world" + System.currentTimeMillis()); 
		try{
			Message res = p.publish(msg);
			System.out.println(res); 
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		Thread.sleep(2000);
		}
		 
		broker.close();
	}

}

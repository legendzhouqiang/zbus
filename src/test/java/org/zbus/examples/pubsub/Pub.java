package org.zbus.examples.pubsub;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class Pub {
	public static void main(String[] args) throws Exception{    
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Producer producer = new Producer(broker, "MyPubSub");
		producer.createMQ();    
		
		for(int i=0;i<100000;i++){
			Message msg = new Message(); 
			msg.setBody("hello world " + i);
				
			producer.sendSync(msg); 
			if((i+1)%10000==0){
				System.out.println(i+1);
			}
		} 
		
		broker.close();
	} 
}

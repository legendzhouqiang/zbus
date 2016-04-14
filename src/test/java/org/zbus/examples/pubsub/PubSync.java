package org.zbus.examples.pubsub;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;

public class PubSync {
	public static void main(String[] args) throws Exception{    
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ();  
		
		String[] topics = {"sse", "google", "qhee"};
		
		for(int i=0;i<10;i++){
			Message msg = new Message();
			msg.setTopic(topics[i%3]); 
			msg.setBody("hello world");
				
			producer.sendSync(msg); 
		}
		
		
		broker.close();
	} 
}

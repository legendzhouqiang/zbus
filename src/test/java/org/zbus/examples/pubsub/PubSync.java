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
		
		for(int i=0;i<10000;i++){
			Message msg = new Message();
			msg.setTopic("zbus"); 
			msg.setBody("hello world" + i);
				
			producer.sendSync(msg); 
		} 
		
		broker.close();
	} 
}

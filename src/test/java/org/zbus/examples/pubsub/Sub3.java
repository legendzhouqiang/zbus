package org.zbus.examples.pubsub;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.net.http.Message;

public class Sub3 {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		
		Consumer c = new Consumer(broker, "MyMQ");   
		c.setConsumeGroup("Group3");  
		c.createMQ();
		
		while(true){ //take one by one, controlled by caller
			Message message = c.take();
			System.out.println(message);
		}
	} 
}

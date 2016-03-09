package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.net.http.Message;

public class ConsumerTakeMessage { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555"); 
 
		Consumer consumer = new Consumer(broker, "MyMQ"); 
		
		while(true){
			Message msg = consumer.take();
			System.out.println(msg);
		} 
	}
}

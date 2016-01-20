package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.net.http.Message;

public class ConsumerTakeMessage { 
	
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker(); // default to 127.0.0.1:15555
 
		Consumer consumer = new Consumer(broker, "MyMQ"); 
		
		Message msg = consumer.take();
		System.out.println(msg);
		
		consumer.close();
		broker.close();
	}
}

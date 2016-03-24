package org.zbus.examples.mq.timer;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerDelayExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker(); // default to 127.0.0.1:15555

		Producer producer = new Producer(broker, "MyMQ"); 
		producer.createMQ();

		Message msg = new Message(); 
		
		msg.setDelay("10s"); //delay 10 seconds to be able to deliver to consumer
		//msg.setDelay("2016-03-24 15:05:00"); //you can also set absolute value
		
		msg.setBody("hello world"); 
		msg = producer.sendSync(msg);
		System.out.println(msg);

		broker.close();
	}
}

package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker(); // default to 127.0.0.1:15555

		Producer producer = new Producer(broker, "MyMQ"); 
		producer.createMQ();

		Message msg = new Message(); 
		msg.setBody("hello world"); 
		msg = producer.sendSync(msg);
		System.out.println(msg);

		broker.close();
	}
}

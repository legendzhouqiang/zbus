package io.zbus.examples.mq;

import io.zbus.mq.Broker;
import io.zbus.mq.Producer;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class ProducerExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		Producer p = new Producer(broker, "MyMQ");
		p.declareQueue();

		Message msg = new Message();   
		msg.setBody("hello world " + System.currentTimeMillis());
		msg = p.produce(msg);
		
		System.out.println(msg);

		broker.close();
	}
}

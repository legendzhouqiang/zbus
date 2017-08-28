package org.zbus.examples.delay_ttl;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerTtlExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		Producer producer = new Producer(broker, "MyMQ2");
		producer.createMQ();

		Message msg = new Message();
		msg.setTtl("10s");
		msg.setBody("hello world " + System.currentTimeMillis());
		msg = producer.sendSync(msg);
		System.out.println(msg);

		broker.close();
	}
}

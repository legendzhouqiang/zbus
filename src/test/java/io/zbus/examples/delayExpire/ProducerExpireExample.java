package io.zbus.examples.delayExpire;

import java.util.concurrent.TimeUnit;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;
import io.zbus.mq.Producer;
import io.zbus.net.http.Message;

public class ProducerExpireExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		Producer producer = new Producer(broker, "MyMQ");
		producer.declareMQ();

		Message msg = new Message();
		msg.setTtl(10, TimeUnit.SECONDS);
		
		msg.setBody("hello world " + System.currentTimeMillis());
		
		msg = producer.sendSync(msg);
		System.out.println(msg);

		broker.close();
	}
}

package io.zbus.examples.mq;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.broker.ZbusBroker;

public class ProducerPressure {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		Producer producer = new Producer(broker, "MyMQ");
		producer.declareQueue();

		for(int i=0;i<100000000;i++){
			Message msg = new Message();
			//msg.setAck(false);
			msg.setBody("hello world " + i);
			//producer.sendAsync(msg);
			producer.produce(msg);
			//System.out.println(msg);
		}
		
		broker.close();
		System.err.println("done");
	}
}

package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerPressure {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ();

		for(int i=0;i<100;i++){
			Message msg = new Message();
			//msg.setAck(false);
			msg.setBody("hello world " + i);
			//producer.sendAsync(msg);
			producer.sendSync(msg);
			//System.out.println(msg);
		}
		
		broker.close();
		System.err.println("done");
	}
}

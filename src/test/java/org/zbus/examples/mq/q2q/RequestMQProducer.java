package org.zbus.examples.mq.q2q;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class RequestMQProducer {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker(); // default to 127.0.0.1:15555

		Producer producer = new Producer(broker, "RequestMQ");
		producer.createMQ();

		int i = 0;
		while(true){
			Message msg = new Message();
			msg.setBody("hello world " + i++); 
			msg = producer.sendSync(msg);
			System.out.println(msg);
			
			Thread.sleep(5000);
		} 
	}
}

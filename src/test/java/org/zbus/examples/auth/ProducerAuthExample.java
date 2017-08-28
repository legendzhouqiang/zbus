package org.zbus.examples.auth;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerAuthExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker("127.0.0.1:15555");

		MqConfig config = new MqConfig();
		config.setMq("MyMQ_Auth");
		config.setAccessToken("MyMQ_Token2"); //token to access this queue
		config.setBroker(broker);
		
		Producer producer = new Producer(config); 
		producer.createMQ(); 
		
		Message msg = new Message(); 
		msg.setBody("hello world " + System.currentTimeMillis());
		msg = producer.sendSync(msg);
		System.out.println(msg);

		broker.close();
	}
}

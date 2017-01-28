package io.zbus.examples.broker;

import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.Broker;
import io.zbus.mq.broker.TrackBroker;

public class TrackBrokerTest {
	
	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new TrackBroker(config);
		
		Producer producer = new Producer(broker, "Mq2");
		producer.declareTopic();
		
		Message msg = new Message();
		msg.setBody("test ok");
		msg = producer.publish(msg);
		System.out.println(msg);
		
		broker.close();
	}
}

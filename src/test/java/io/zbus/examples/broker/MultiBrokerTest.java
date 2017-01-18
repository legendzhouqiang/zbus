package io.zbus.examples.broker;

import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Broker;
import io.zbus.mq.broker.MultiBroker;

public class MultiBrokerTest {
	
	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555;127.0.0.1:15556");
		Broker broker = new MultiBroker(config);
		
		
		
		broker.close();
	}
}

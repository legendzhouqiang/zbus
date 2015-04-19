package org.zbus.broker;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;

public class SingleBrokerTest {

	public static void main(String[] args) throws Exception {  
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:80");
		Broker broker = new SingleBroker(config);
		
	
		broker.close();
	}
}

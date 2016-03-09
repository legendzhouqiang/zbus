package org.zbus.examples.broker;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;

public class BrokerExample {

	public static void main(String[] args) throws Exception { 
		Broker broker = new ZbusBroker("jvm"); //JvmBroker
		broker.close();
		
		broker = new ZbusBroker("127.0.0.1:15555"); //SingleBroker
		broker.close();
		
		broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667"); //HaBroker
		broker.close();
		
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		
		broker = new ZbusBroker(config);
		broker.close();

	}

}

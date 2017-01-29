package io.zbus.broker;

import io.zbus.mq.Broker;
import io.zbus.mq.ZbusBroker;

public class BrokerTest {

	public static void main(String[] args) throws Exception { 
		Broker broker = new ZbusBroker("127.0.0.1:15555"); 
 

		broker.close(); 
	}

}

package io.zbus.examples.broker;

import io.zbus.mq.Broker;
import io.zbus.mq.ZbusBroker;

public class BrokerExample {

	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555"); 
		broker.close();
		
		broker = new ZbusBroker("jvm");
		broker.close();
	} 
	
}

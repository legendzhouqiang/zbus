package io.zbus.examples.broker;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;

public class BrokerExample {

	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker(); 
		broker.close();
		
		broker = new ZbusBroker("jvm");
		broker.close();
	} 
	
}

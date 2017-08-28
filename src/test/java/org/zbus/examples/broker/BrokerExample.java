package org.zbus.examples.broker;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;

public class BrokerExample {

	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker(); 
		broker.close();
		
		broker = new ZbusBroker("jvm");
		broker.close();
	} 
	
}

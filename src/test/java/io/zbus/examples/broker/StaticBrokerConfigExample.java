package io.zbus.examples.broker;

import io.zbus.mq.Broker;

public class StaticBrokerConfigExample {
 
	public static void main(String[] args) throws Exception {
		Broker broker = new Broker(); 
		//static add server in this broker, add will block to connected to server or exception raised.
		broker.addServer("localhost:15555"); 
		broker.addServer("localhost:15556");
		broker.addServer("localhost:15557"); 
		
		
		broker.close();
	}

}

package io.zbus.examples.broker;

import io.zbus.mq.Broker;

public class BrokerExample {
 
	public static void main(String[] args) throws Exception {	 
		Broker broker = new Broker();  
		broker.addTracker("localhost:15555", "ssl/zbus.crt");
		
		//will connect to all servers tracked by server: localhost:15555 
		broker.close();
	}

}

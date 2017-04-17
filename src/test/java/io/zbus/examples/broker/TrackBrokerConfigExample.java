package io.zbus.examples.broker;

import io.zbus.mq.Broker;

public class TrackBrokerConfigExample {
 
	public static void main(String[] args) throws Exception {	
		Broker broker = new Broker("localhost:15555");   
		//will connect to all servers tracked by server: localhost:15555
		
		broker.close();
	}

}

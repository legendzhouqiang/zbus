package org.zbus.examples.keys;

import org.zbus.broker.Broker;
import org.zbus.broker.LocalBroker;
import org.zbus.mq.MqAdmin;

public class KeyExample {

	public static void main(String[] args) throws Exception {
		//this broker is shared among same JVM process
		Broker broker = new LocalBroker(); 
  
		MqAdmin p = new MqAdmin(broker, "MyMQ");
		
		int res = p.addKey("group", "test");
		System.out.println(res);
		res = p.removeKey("test");
		System.out.println(res);
		res = p.addKey("test");
		System.out.println(res);
		
		
		broker.close();
	}

}

package org.zbus.examples.keys;

import org.zbus.broker.Broker;
import org.zbus.broker.JvmBroker;
import org.zbus.mq.MqAdmin;

public class KeyExample {

	public static void main(String[] args) throws Exception {
		//this broker is shared among same JVM process
		Broker broker = new JvmBroker(); 
  
		MqAdmin p = new MqAdmin(broker, "MyMQ");
		
		int res = p.addKey("group", "test"); 
		System.out.println(res);
		res = p.removeGroup("group");
		System.out.println(res);
		res = p.addKey("group", "test");
		System.out.println(res);
		
		
		broker.close();
	}

}

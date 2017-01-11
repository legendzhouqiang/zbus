package io.zbus.examples.mq;

import io.zbus.broker.Broker;
import io.zbus.broker.ZbusBroker;
import io.zbus.mq.MqAdmin;

public class MqAdminExample { 
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");    
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.declareMQ();
		} 
		
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.removeMQ();
		} 
		
		broker.close();
	}
}

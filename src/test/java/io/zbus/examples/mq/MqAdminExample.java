package io.zbus.examples.mq;

import io.zbus.mq.Broker;
import io.zbus.mq.MqAdmin;
import io.zbus.mq.broker.ZbusBroker;

public class MqAdminExample { 
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");    
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.declareQueue();
		} 
		
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.removeQueue();
		} 
		
		broker.close();
	}
}

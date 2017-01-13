package io.zbus.examples.mq;

import io.zbus.mq.Broker;
import io.zbus.mq.MqAdmin;
import io.zbus.mq.ZbusBroker;

public class MqAdminExample { 
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");    
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.declareTopic();
		} 
		
		for(int i=0;i<100;i++){
			MqAdmin admin = new MqAdmin(broker, "MyMQ-"+i); 
			admin.removeTopic();
		} 
		
		broker.close();
	}
}

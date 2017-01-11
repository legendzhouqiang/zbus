package io.zbus.examples.pubsub;

import io.zbus.mq.Broker;
import io.zbus.mq.Producer;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class Pub {
	public static void main(String[] args) throws Exception{    
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Producer producer = new Producer(broker, "MyMQ");
		producer.declareQueue();    
		
		final int count = 1;
		for(int i=0;i<count;i++){
			Message msg = new Message(); 
			msg.setTag("abc.zzz.yy");
			msg.setBody("hello world " + i);
				
			producer.sendSync(msg); 
			if((i+1)*10%count==0){
				System.out.println(i+1);
			}
		} 
		
		broker.close();
	} 
}

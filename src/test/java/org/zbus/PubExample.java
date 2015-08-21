package org.zbus;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.Producer;
import org.zbus.mq.SingleBroker;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;

public class PubExample {
	public static void main(String[] args) throws Exception{   
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		 
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ();  
		
		Message msg = new Message();
		msg.setTopic("sse");
		
		for(int i=0;i<1000;i++){
			msg.setBody("hello world"+i);
			producer.sendSync(msg); 
		}
		
		broker.close();
	} 
}

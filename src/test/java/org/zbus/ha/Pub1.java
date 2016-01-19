package org.zbus.ha;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.mq.Producer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;

public class Pub1 {
	public static void main(String[] args) throws Exception{   
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666");
		final Broker broker = new HaBroker(brokerConfig);
		 
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ();  
		
		Message msg = new Message();
		msg.setTopic("sse"); 
		msg.setBody("hello world");
		
		producer.sendSync(msg);   
		
		
		broker.close();
	} 
}

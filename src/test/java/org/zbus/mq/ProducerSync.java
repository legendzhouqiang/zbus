package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;

public class ProducerSync {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建
 
		for(int i=0; i<10; i++){ 
			Message msg = new Message();  
			msg.setBody("hello world"+i);
			msg = producer.sendSync(msg); 
			System.out.println(msg);
		}
		
		broker.close();
	}
}

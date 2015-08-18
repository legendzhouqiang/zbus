package org.zbus;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.Producer;
import org.zbus.mq.SingleBroker;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;

public class PubExample {
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代理【重量级对象，需要释放】
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ(); //创建MQ，如果确定存在的MQ可以不创建
		
		Message msg = new Message();
		msg.setTopic("sse");
		msg.setBody("hello world");
		
		Message res = producer.sendSync(msg);
		System.out.println(res);
		
		broker.close();
	} 
}

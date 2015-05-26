package org.zstacks.zbus;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Producer;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.znet.Message;

public class PubExample {
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyPubSub", MessageMode.PubSub);
		producer.createMQ(); //创建MQ，如果确定存在的MQ可以不创建
		
		Message msg = new Message();
		msg.setTopic("hong");
		msg.setBody("hello world2");
		
		Message res = producer.sendSync(msg, 2500);
		System.out.println(res);
		
		broker.close();
	} 
}

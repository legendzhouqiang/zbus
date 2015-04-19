package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Producer;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.remoting.Message;

public class ProducerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); //如果已经确定存在，不需要创建
		
		Message msg = new Message(); 
		msg.setBody("hello world");  
		Message res = producer.sendSync(msg, 1000);
		System.out.println(res);
		
		//3）销毁Broker
		broker.close();
	} 
}

package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Consumer;
import org.zbus.client.MqConfig;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.broker.SingleBroker;
import org.zbus.remoting.Message;

public class ConsumerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig brokerConfig = new SingleBrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq("MyMQ");
		
		//2) 创建消费者
		Consumer c = new Consumer(config);
		while(true){
			Message msg = c.recv(10000);
			if(msg == null) continue;
			
			System.out.println(msg);
		}
	} 
}

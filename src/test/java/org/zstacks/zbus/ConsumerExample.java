package org.zstacks.zbus;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Consumer;
import org.zstacks.zbus.client.MqConfig;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.znet.Message;
import org.zstacks.znet.callback.MessageCallback;
import org.zstacks.znet.nio.Session;

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
		@SuppressWarnings("resource")
		Consumer c = new Consumer(config);
		
		c.onMessage(new MessageCallback() {
			public void onMessage(Message msg, Session sess) throws IOException {
				System.out.println(msg);
			}
		});
	} 
}

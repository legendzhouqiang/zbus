package org.zbus.ha;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.MqConfig;
import org.zbus.net.http.Message;

public class Consumer2 {
	public static void main(String[] args) throws Exception{  
		//创建Broker代表
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setBrokerAddress("127.0.0.1:15556");
		Broker broker = new ZbusBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq("MyMQ");
		
		//创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(config);  
		
		c.onMessage(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});

		//启动消费线程
		c.start();   
		
	} 
}

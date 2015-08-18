package org.zbus;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.Consumer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.mq.SingleBroker;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageHandler;

public class SubExample {
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代表
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		
		final Broker broker = new SingleBroker(config);
		
		//2) 创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(broker, "MyPubSub", MqMode.PubSub); 
		c.setTopic("sse");
		
		c.onMessage(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				System.out.println(msg);
			}
		});
		
		c.start();
		
	} 
}

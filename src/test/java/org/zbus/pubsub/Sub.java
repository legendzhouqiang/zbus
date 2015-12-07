package org.zbus.pubsub;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;

public class Sub {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代表
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555"); 
		final Broker broker = new SingleBroker(config);
		
		//2) 创建消费者 
		Consumer c = new Consumer(broker, "MyPubSub", MqMode.PubSub); 
		c.setTopic("sse,google,ms"); 
		
		final AtomicLong counter = new AtomicLong(0);
		c.onMessage(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				System.out.println(msg);
				System.out.println(counter.incrementAndGet());
			}
		});
		
		c.start(); 
	} 
}

package org.zbus.mq;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.MqConfig;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;

public class ConsumerExample {
	public static void main(String[] args) throws Exception{  
		//创建Broker代表
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq("MyMQ"); 
		
		final AtomicLong counter = new AtomicLong(0);
		for(int i=0;i<10;i++){
		//创建消费者
			@SuppressWarnings("resource")
			Consumer c = new Consumer(config);  
			c.onMessage(new MessageHandler() { 
				@Override
				public void handle(Message msg, Session sess) throws IOException {
					counter.incrementAndGet();
					long curr = counter.get();
					if(curr %10000 == 0){
						System.out.println("consumed: "+curr);
					}
				}
			});
			
			c.start(); 
		}
	} 
}

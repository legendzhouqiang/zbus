package org.zbus.examples.pubsub;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;

public class Sub {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代表
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555"); 
		final Broker broker = new SingleBroker(config);
		
		//2) 创建消费者 
		Consumer c = new Consumer(broker, "MyPubSub", MqMode.PubSub); 
		final String topic = "sse";
		c.setTopic(topic); 
		
		c.onMessage(new ConsumerHandler() { 
			int count = 0;
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException {  
				if(topic.equals(msg.getTopic())){
					count++;
					System.out.println(topic+":" + count);
				} else {
					System.out.println(msg);
				}
			}
		});
		
		c.start(); 
	} 
}

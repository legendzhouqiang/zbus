package org.zstacks.zbus;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Consumer;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.znet.Message;
import org.zstacks.znet.callback.MessageCallback;
import org.zstacks.znet.nio.Session;

public class SubExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		
		final Broker broker = new SingleBroker(config);
		
		//2) 创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(broker, "MyPubSub", MessageMode.PubSub); 
		c.setTopic("sse");
		
		c.onMessage(new MessageCallback() { 
			public void onMessage(Message msg, Session sess) throws IOException {
				System.out.println(msg); 
				
			}
		}); 
	} 
}

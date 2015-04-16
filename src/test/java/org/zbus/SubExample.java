package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Consumer;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.protocol.MessageMode;
import org.zbus.remoting.Message;
import org.zbus.remoting.callback.MessageCallback;
import org.zbus.remoting.nio.Session;

public class SubExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		
		final Broker broker = new SingleBroker(config);
		
		//2) 创建消费者
		Consumer c = new Consumer(broker, "MyPubSub", MessageMode.PubSub); 
		c.setTopic("hong");
		
		c.onMessage(new MessageCallback() { 
			@Override
			public void onMessage(Message msg, Session sess) throws IOException {
				System.out.println(msg); 
				
			}
		}); 
	} 
}

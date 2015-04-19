package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.service.Caller;
import org.zbus.remoting.Message;

public class CallerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 【轻量级对象，不需要释放，随便使用】
		Caller caller = new Caller(broker, "MyService");
		
		Message msg = new Message();
		msg.setBody("hello world");
		
		Message res = caller.invokeSync(msg, 2500);
		System.out.println(res);
		
		broker.close();
	} 
}

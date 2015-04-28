package org.zstacks.zbus;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

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
		for(int i=0;i<1;i++){
		Message res = caller.invokeSync(msg, 2500);
		System.out.println(res);}
		
		broker.close();
	} 
}

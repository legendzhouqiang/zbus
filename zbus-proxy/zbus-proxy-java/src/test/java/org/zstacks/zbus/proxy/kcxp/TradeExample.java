package org.zstacks.zbus.proxy.kcxp;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

public class TradeExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		
		Caller c = new Caller(broker, "Trade");
		
		for(int i=0;i<10000;i++){
		Message req = new Message();
		req.setBody("multi-zbus-to-msmq");
		Message res = c.invokeSync(req, 2500);
		System.out.println(res);
		}
		broker.close();
	} 
}

package org.zstacks.zbus.proxy.kcxp;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.proxy.trade.TradeClient;

public class TradeCryptExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		TradeClient client = new TradeClient(broker, "Trade");  
		String password = client.encrypt("KDE", "123456", "110000001804");
		System.out.println(password); //q+4ooPSsA5oAnx+fwv2k4g==
		
		password = client.decrypt("IKNSg6K7twOgLcqnouCKePNyv3XJbiCmS6esaMr+uTZr8c9RTgc/YwsivG35Yc1UWUf9q5wFqetLkE0vdoUg4I==");
		System.out.println(password); 
		
	} 
}

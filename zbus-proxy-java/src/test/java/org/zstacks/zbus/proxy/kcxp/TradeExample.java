package org.zstacks.zbus.proxy.kcxp;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.proxy.trade.Request;
import org.zstacks.zbus.proxy.trade.Response;
import org.zstacks.zbus.proxy.trade.TradeClient;

public class TradeExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		

		TradeClient client = new TradeClient(broker, "Trade");  
		Request t = new Request();
		t.funcId = "420301";
		t.tradeNodeId = "";
		t.sessionId = "";
		t.userInfo = "0~~127.0.0.1~1100";
		 
		t.loginType = "Z";
		t.loginId = "110000001804";
		t.custOrg = "1100"; 
		t.operIp = "172.16.8.107";
		t.operOrg = "yyt";
		t.operType = "g"; 
		
		
		
		String password = client.encrypt("KDE", "123456", "110000001804");
		System.out.println(password);
		
		t.params.add("Z");
		t.params.add("110000001804");
		t.params.add(password);

		 
		Response res = client.trade(t);
		System.out.println(res.toString());
		
	} 
}

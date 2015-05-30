package org.zstacks.zbus;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.MqAdmin;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.protocol.MqInfo;

public class QueryMqExample {
	public static void main(String[] args) throws Exception{   
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 【轻量级对象，不需要释放，随便使用】
		MqAdmin admin = new MqAdmin(broker, "MyService");
		MqInfo mqInfo = admin.queryMQ();
		System.out.println(mqInfo);
		
		broker.close();
	} 
}

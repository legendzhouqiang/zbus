package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;

public class QueryMqExample {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		MqAdmin admin = new MqAdmin(broker, "MyMQ");
		
		String res = admin.queryMQ(); //保持zbus底层不依赖json包，使用者再解释json
		System.out.println(res);
		
		broker.close();
	}
}

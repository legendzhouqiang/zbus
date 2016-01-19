package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;

public class ConsumerExample {
	public static void main(String[] args) throws Exception {
		// 创建Broker代表
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);

		MqConfig config = new MqConfig();
		config.setBroker(broker);
		config.setMq("MyMQ");

		// 创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(config);
		while(true){
			Message message = c.take();
			System.out.println(message);
		}

	}
}

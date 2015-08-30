package org.zbus.ha;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerExample {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666");
		Broker broker = new HaBroker(brokerConfig);
 
		Producer producer = new Producer(broker, "MyMQ");
		//producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		Message msg = new Message();
		msg.setBody("hello world");
		for(int i=0;i<1;i++){
			producer.sendSync(msg);  
		}
		
		//销毁Broker
		broker.close();
	}
}

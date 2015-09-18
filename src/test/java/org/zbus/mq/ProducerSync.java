package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerSync {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("10.8.60.250:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		
		long total = 0;
		for(int i=0;i<100000;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message();
			msg.setBody("hello world"+i);
			producer.sendSync(msg);  
			long end = System.currentTimeMillis();
			total += (end-start);
			System.out.format("Time: %.1f\n", total*1.0/(i+1));
		}
		
		broker.close();
	}
}

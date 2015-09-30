package org.zbus.perf;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;

public class ConsumerPerf {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Consumer c = new Consumer(broker, "MyMQ");
		//创建消息，消息体可以是任意binary，应用协议交给使用者
		
		long total = 0;
		for(int i=0;i<1000000;i++){
			long start = System.currentTimeMillis();
			c.recv(10000);
			long end = System.currentTimeMillis();
			total += (end-start);
			System.out.format("Time: %.4f\n", total*1.0/(i+1));
		}
		
		c.close();
		broker.close();
	}
}

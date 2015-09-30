package org.zbus.perf;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class ProducerLatencyPerf {
	public static void main(String[] args) throws Exception { 
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress(serverAddress);
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, mq);
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		
		long total = 0;
		for(int i=0;i<loopCount;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message();
			msg.setBody("hello world"+i);
			producer.sendSync(msg);  
			long end = System.currentTimeMillis();
			total += (end-start);
			System.out.format("Time: %.4f\n", total*1.0/(i+1));
		}
		
		broker.close();
	}
}

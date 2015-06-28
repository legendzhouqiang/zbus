package org.zstacks.zbus;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Producer;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.znet.Message;

public class ProducerExample {
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); //如果已经确定存在，不需要创建
		
		for(int i=0;i<1;i++){
			Message msg = new Message(); 
			msg.setBody("hello world");  
			Message res = producer.sendSync(msg, 1000);
			System.out.println(res); 
			if((i+1) % 1000 == 0){
				System.out.println("done: "+ i);
			}
		}
		
		//3）销毁Broker
		broker.close();
	} 
}

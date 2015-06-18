package org.zstacks.zbus;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Producer;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.znet.Message;
import org.zstacks.znet.ticket.ResultCallback;

public class ProducerExample {
	public static void main(String[] args) throws Exception{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); //如果已经确定存在，不需要创建
		
		for(int i=0;i<10;i++){
		Message msg = new Message(); 
		msg.setBody("hello world");  
		//Message res = producer.sendSync(msg, 1000);
		//System.out.println(res);
		
		producer.send(msg, new ResultCallback() {
			@Override
			public void onCompleted(Message msg) { 
				System.out.println(msg);
			}
		});
		}
		
		//3）销毁Broker
		//broker.close();
	} 
}

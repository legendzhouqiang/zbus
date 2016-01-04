package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;

public class ProducerAsync {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		
		for(int i=0;i<100000;i++){
			Message msg = new Message();
			msg.setBody("hello world"+i); 
			producer.invokeAsync(msg, new ResultCallback<Message>() { 
				@Override
				public void onReturn(Message result) { 
					System.out.println(result);
				}
			});
		}
		
		System.out.println("---done---");
		//销毁Broker, 异步发送需要Sleep等待
		//broker.close();
	}
}

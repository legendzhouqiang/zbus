package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Producer;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

public class PubExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyPubSub");
		
		Message msg = new Message();
		msg.setTopic("hong");
		msg.setBody("hello world");
		
		producer.send(msg, new ResultCallback() {
			@Override
			public void onCompleted(Message result) {
				System.out.println(result);
				
				//收到消息退出应用，清理broker
				broker.destroy();
			}
		});
	} 
}

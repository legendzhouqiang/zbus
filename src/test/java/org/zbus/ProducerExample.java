package org.zbus;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.zbus.client.Broker;
import org.zbus.client.Producer;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.broker.SingleBroker;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

public class ProducerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		
		//2) 创建生产者
		Producer producer = new Producer(broker, "Plugin");
		final AtomicInteger counter = new AtomicInteger();
		for(int i=0;i<100000;i++){
			Message msg = new Message();
			msg.setBody("hello world %6d",i);
			
			producer.send(msg, new ResultCallback() {
				@Override
				public void onCompleted(Message result) { 
					System.out.println(counter.incrementAndGet());
				}
			});
		}
	} 
}

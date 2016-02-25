package org.zbus.examples.jvm;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class LocalExample {

	public static void main(String[] args) throws Exception {
		//this broker is shared among same JVM process
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("jvm"); 
		Broker broker = new ZbusBroker(config);  //equal to new JvmBroker
		
 
		Consumer consumer = new Consumer(broker, "MyMQ"); 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ();
		
		for(int i=0;i<10;i++){
			Message msg = new Message();
			msg.setBody("hello world"+i); 
			producer.sendSync(msg);
			Thread.sleep(100);
		} 
		
		consumer.close();
		broker.close();
	}

}

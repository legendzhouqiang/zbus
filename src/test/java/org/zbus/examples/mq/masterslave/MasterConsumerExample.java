package org.zbus.examples.mq.masterslave;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.net.http.Message;

public class MasterConsumerExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new ZbusBroker(config); 
 
		Consumer consumer = new Consumer(broker, "MasterMQ"); 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				
				System.out.println(msg);
			}
		});    
	}
}

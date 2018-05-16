package org.zbus.examples.broker;

import java.io.IOException;
 
import org.zbus.broker.Broker;
import org.zbus.broker.JvmBroker;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class JvmBrokerExample {
	private static final Logger logger = LoggerFactory.getLogger(JvmBrokerExample.class);
	public static void main(String[] args) throws Exception {  
		Broker broker = new JvmBroker();
		
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.createMQ();
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				logger.info(""+msg);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		for(int i=0;i<2;i++) {
			Message message = new Message();
			message.setBody("test body");
			producer.sendAsync(message); 
		} 
		
	} 
	
}

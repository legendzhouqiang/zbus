package org.zbus.examples.mq.q2q;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.net.http.Message;

public class ReplyMQConsumer { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker(); // default to 127.0.0.1:15555
 
		Consumer consumer = new Consumer(broker, "ReplyMQ"); 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});    
	}
}

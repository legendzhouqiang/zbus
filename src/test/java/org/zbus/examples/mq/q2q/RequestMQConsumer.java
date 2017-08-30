package org.zbus.examples.mq.q2q;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class RequestMQConsumer { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Broker broker = new ZbusBroker(); // default to 127.0.0.1:15555
 
		Consumer consumer = new Consumer(broker, "RequestMQ"); 
		final Producer replyProducer = new Producer(broker, "ReplyMQ");
		replyProducer.createMQ();
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
				
				replyProducer.sendAsync(msg);
			}
		});    
	}
}

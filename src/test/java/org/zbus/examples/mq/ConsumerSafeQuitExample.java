package org.zbus.examples.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.net.http.Message;

/**
 * Consumer should clean resource correctly, try this example
 * 
 * @author rushmore (洪磊明)
 *
 */
public class ConsumerSafeQuitExample { 
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker(); // default to 127.0.0.1:15555
 
		Consumer consumer = new Consumer(broker, "MyMQ"); 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				
				System.out.println(msg);
			}
		});   
		//stop consumer, equally to call consumer.stop()
		consumer.close();
		//clear broker
		broker.close();
	}
}

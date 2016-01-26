package org.zbus.examples.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;

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
		consumer.start(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				System.out.println(msg);
			}
		});   
		//stop consumer, equally to call consumer.stop()
		consumer.close();
		//clear broker
		broker.close();
	}
}

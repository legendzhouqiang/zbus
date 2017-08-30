package org.zbus.examples.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;
import org.zbus.net.http.Message;

public class ConsumerTakeMessageFailure {

	static Thread createConsumerThread(final Broker broker) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				Consumer consumer = new Consumer(broker, "MyMQ");
				while (true) {
					Message msg;
					try {
						msg = consumer.take();
						System.out.println(msg);
					} catch (InterruptedException e) { 
						//break;
					} catch (IOException e) {
						break;
					}

				} 
				try {
					consumer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		return thread;
	}

	public static void main(String[] args) throws Exception {
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		Thread[] threads = new Thread[100];
		for(int i=0; i<100; i++){
			Thread thread = createConsumerThread(broker);
			thread.start();
			threads[i] = thread; 
		}
		
		for(int i=0; i<100; i++){ 
			
			if(i>10){
				threads[i-10].interrupt();
				Thread.sleep(1000); 
			}
		}
		
		
	}
}

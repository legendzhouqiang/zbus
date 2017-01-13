package io.zbus.examples.mq;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;

public class ConsumerHandlerRunInPool { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.setConsumeThreadPoolSize(1000); 
		consumer.setConsumeInPool(true); //enable consumer handler run in thread
		
		final AtomicLong msgCounter = new AtomicLong(0);
		final long start = System.currentTimeMillis();
		consumer.start(new ConsumerHandler() { 
			Random r = new Random();
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				
				try {
					Thread.sleep(r.nextInt(100)); //emulates work costs around 100ms
				} catch (InterruptedException e) {
					//
				}
				long count = msgCounter.getAndIncrement();
				if(count%1000 == 0){
					long ms = System.currentTimeMillis()-start;
					System.out.format("QPS: %.2f\n", msgCounter.get()*1000.0/ms); 
				} 
			}
		});    
	}
}

package io.zbus.performance;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.broker.Broker;
import io.zbus.broker.BrokerConfig;
import io.zbus.broker.ZbusBroker;
import io.zbus.kit.ConfigKit;
import io.zbus.kit.log.Logger;
import io.zbus.kit.log.LoggerFactory;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Consumer.ConsumerHandler;
import io.zbus.net.http.Message;

public class ConsumerPerf {
	private static final Logger log = LoggerFactory.getLogger(ConsumerPerf.class); 
	public static void main(String[] args) throws Exception{  
		
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 16); 
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		final int interval = ConfigKit.option(args, "-int", 10000); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		ConsumerConfig config = new ConsumerConfig(); 
		config.setBroker(broker);
		config.setMq(mq); 
		
		final AtomicLong counter = new AtomicLong(0);
		final AtomicLong lastMark = new AtomicLong(System.currentTimeMillis());
		for(int i=0;i<threadCount;i++){ 
			@SuppressWarnings("resource")
			Consumer c = new Consumer(config);  
			c.onMessage(new ConsumerHandler() { 
				@Override
				public void handle(Message msg, Consumer consumer) throws IOException { 
					counter.incrementAndGet();
					long curr = counter.get();
					if(curr %interval == 0){
						long start = lastMark.get();
						lastMark.set(System.currentTimeMillis());
						long end = System.currentTimeMillis();
						log.info(""+msg);
						log.info("Consumed:%d, QPS: %.4f", curr, interval*1000.0/(end-start) );
					}
				}
			}); 
			c.start(); 
		}
	} 
}

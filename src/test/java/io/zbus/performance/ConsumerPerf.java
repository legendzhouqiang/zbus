package io.zbus.performance;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.ZbusBroker;
import io.zbus.util.ConfigUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class ConsumerPerf {
	private static final Logger log = LoggerFactory.getLogger(ConsumerPerf.class); 
	public static void main(String[] args) throws Exception{  
		
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigUtil.option(args, "-c", 16); 
		final String mq = ConfigUtil.option(args, "-mq", "MyMQ");
		final int interval = ConfigUtil.option(args, "-int", 10000); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setTopic(mq); 
		
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

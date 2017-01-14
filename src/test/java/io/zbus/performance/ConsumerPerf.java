package io.zbus.performance;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerHandler;
import io.zbus.mq.ConsumerService;
import io.zbus.mq.ConsumerServiceConfig;
import io.zbus.mq.Message;
import io.zbus.mq.ZbusBroker;
import io.zbus.util.ConfigUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class ConsumerPerf {
	private static final Logger log = LoggerFactory.getLogger(ConsumerPerf.class); 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{  
		
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigUtil.option(args, "-c", 16); 
		final String topic = ConfigUtil.option(args, "-mq", "MyMQ");
		final int interval = ConfigUtil.option(args, "-int", 10000); 
		
		final AtomicLong counter = new AtomicLong(0);
		final AtomicLong lastMark = new AtomicLong(System.currentTimeMillis()); 
		
		Broker broker = new ZbusBroker(serverAddress);
		
		ConsumerServiceConfig config = new ConsumerServiceConfig(broker); 
		config.setBroker(broker);
		config.setTopic(topic); 
		config.setConsumerCount(threadCount); 
		config.setConsumerHandler(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				counter.incrementAndGet();
				long curr = counter.get();
				if(curr %interval == 0){
					long start = lastMark.get();
					lastMark.set(System.currentTimeMillis());
					long end = System.currentTimeMillis();
					log.info(""+msg.getOffset());
					log.info("Consumed:%d, QPS: %.4f", curr, interval*1000.0/(end-start) );
				}
			}
		});   
		
		ConsumerService service = new ConsumerService(config);
		service.start();
	} 
}

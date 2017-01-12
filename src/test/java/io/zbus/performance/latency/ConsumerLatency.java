package io.zbus.performance.latency;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.performance.Perf;
import io.zbus.util.ConfigUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class ConsumerLatency {
	private static final Logger log = LoggerFactory.getLogger(ConsumerLatency.class);
	public static void main(String[] args) throws Exception {  
		final int loopCount = ConfigUtil.option(args, "-loop", 1000000);  
		final String mq = ConfigUtil.option(args, "-mq", "MyMQ");
		
		final Broker broker = Perf.buildBroker(args);
 
		Consumer c = new Consumer(broker, mq); 
		long total = 0;
		for(int i=0;i<loopCount;i++){
			long start = System.currentTimeMillis();
			c.take(10000);
			long end = System.currentTimeMillis();
			total += (end-start);
			if(i%1000 == 0){
				log.info("Time: %.4f", total*1.0/(i+1));
			}
		}
		
		c.close();
		broker.close();
	}
}

package org.zbus.performance.latency;

import org.zbus.broker.Broker;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.mq.Consumer;
import org.zbus.performance.Perf;

public class ConsumerLatency {
	private static final Logger log = Logger.getLogger(ConsumerLatency.class);
	public static void main(String[] args) throws Exception {  
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		
		final Broker broker = Perf.buildBroker(args);
 
		Consumer c = new Consumer(broker, mq); 
		long total = 0;
		for(int i=0;i<loopCount;i++){
			long start = System.currentTimeMillis();
			c.recv(10000);
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

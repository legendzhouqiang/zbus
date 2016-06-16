package org.zbus.performance.latency;

import org.zbus.broker.Broker;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;
import org.zbus.performance.Perf;

public class ProducerLatency {
	private static final Logger log = LoggerFactory.getLogger(ProducerLatency.class);
	public static void main(String[] args) throws Exception { 
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		
		final Broker broker = Perf.buildBroker(args);
 
		Producer producer = new Producer(broker, mq);
		producer.createMQ(); 
  
		long total = 0;
		for(int i=0;i<loopCount;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message();
			msg.setBody("hello world"+i);
			producer.sendSync(msg);  
			long end = System.currentTimeMillis();
			total += (end-start);
			if(i%1000 == 0){
				log.info("Time: %.4f", total*1.0/(i+1));
			}
		}
		
		broker.close();
	}
}

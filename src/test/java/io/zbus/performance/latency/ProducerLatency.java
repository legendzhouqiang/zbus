package io.zbus.performance.latency;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.log.Logger;
import io.zbus.kit.log.LoggerFactory;
import io.zbus.mq.Broker;
import io.zbus.mq.Producer;
import io.zbus.net.http.Message;
import io.zbus.performance.Perf;

public class ProducerLatency {
	private static final Logger log = LoggerFactory.getLogger(ProducerLatency.class);
	public static void main(String[] args) throws Exception { 
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		
		final Broker broker = Perf.buildBroker(args);
 
		Producer producer = new Producer(broker, mq);
		producer.declareQueue(); 
  
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

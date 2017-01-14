package io.zbus.performance.latency;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.ZbusBroker;
import io.zbus.rpc.mq.MqInvoker;
import io.zbus.util.ConfigUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class ReqRepLatency {
	private static final Logger log = LoggerFactory.getLogger(ProducerLatency.class);
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final int loopCount = ConfigUtil.option(args, "-loop", 1000000);  
		final String mq = ConfigUtil.option(args, "-mq", "ReqRep");
		 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress(serverAddress);
		final Broker broker = new ZbusBroker(config);
		 
		MessageInvoker invoker = new MqInvoker(broker, mq);
		//MessageInvoker invoker = broker;
		
		long total = 0;
		for(int i=0;i<loopCount;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message(); 
			msg.setBody("hello world"+i);
			invoker.invokeSync(msg, 2500);
			long end = System.currentTimeMillis();
			total += (end-start);
			if(i%1000 == 0){
				log.info("Time: %.4f", total*1.0/(i+1));
			}
		} 
		invoker.close();
		broker.close();
	} 
	
}

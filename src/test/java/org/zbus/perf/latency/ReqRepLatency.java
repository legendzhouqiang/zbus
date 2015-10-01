package org.zbus.perf.latency;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class ReqRepLatency {
	private static final Logger log = Logger.getLogger(ProducerLatency.class);
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "ReqRep");
		 
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress(serverAddress);
		final Broker broker = new SingleBroker(config);
		
		//基于MQ的调用
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
		
		broker.close();
	} 
	
}

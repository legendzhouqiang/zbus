package org.zbus.performance.latency;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class ReqRepLatency {
	
	public static void main(String[] args) throws Exception{   
		//配置Broker
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555"); 
		Broker broker = new SingleBroker(brokerConfig);
		
		//基于MQ的调用
		MessageInvoker invoker = new MqInvoker(broker, "ReqRep");
		//MessageInvoker invoker = broker;
		
		long total = 0;
		for(int i=0;i<100000;i++){
			long start = System.currentTimeMillis();
			Message msg = new Message(); 
			msg.setBody("hello world"+i);
			invoker.invokeSync(msg, 2500);
			long end = System.currentTimeMillis();
			total += (end-start);
			System.out.format("Time: %.4f\n", total*1.0/(i+1));
		} 
		
		broker.close();
	} 
	
}

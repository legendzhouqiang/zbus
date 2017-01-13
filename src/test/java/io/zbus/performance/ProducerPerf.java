package io.zbus.performance;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.Producer;
import io.zbus.mq.ZbusBroker;
import io.zbus.util.ConfigUtil;

public class ProducerPerf {
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigUtil.option(args, "-c", 16); 
		final int loopCount = ConfigUtil.option(args, "-loop", 100000);
		final int logCount = ConfigUtil.option(args, "-log", 10000);
		final int msgSize = ConfigUtil.option(args, "-msgSize", 10240);
		final String mq = ConfigUtil.option(args, "-mq", "MyMQ"); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		final MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setTopic(mq); 
		
		Perf perf = new Perf(){ 
			
			@Override
			public TaskInThread buildTaskInThread() {
				return new TaskInThread(){
					Producer producer = new Producer(config); 
					
					@Override
					public void initTask() throws Exception {
						producer.declareTopic();
					}
					
					@Override
					public void doTask() throws Exception {
						Message msg = new Message();
						
						msg.setBody(new byte[msgSize]); 
						msg = producer.publish(msg); 
					}
				};
			} 
			
		}; 
		perf.loopCount = loopCount;
		perf.threadCount = threadCount;
		perf.logInterval = logCount;
		perf.run();
		
		perf.close();
		broker.close();
	} 
}

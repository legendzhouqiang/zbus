package io.zbus.performance;

import io.zbus.kit.ConfigKit;
import io.zbus.mq.Broker;
import io.zbus.mq.MqConfig;
import io.zbus.mq.Producer;
import io.zbus.mq.broker.BrokerConfig;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.net.http.Message;

public class ProducerPerf {
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 16); 
		final int loopCount = ConfigKit.option(args, "-loop", 100000);
		final int logCount = ConfigKit.option(args, "-log", 10000);
		final int msgSize = ConfigKit.option(args, "-msgSize", 10240);
		final String mq = ConfigKit.option(args, "-mq", "MyMQ"); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		final MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq(mq); 
		
		Perf perf = new Perf(){ 
			
			@Override
			public TaskInThread buildTaskInThread() {
				return new TaskInThread(){
					Producer producer = new Producer(config); 
					
					@Override
					public void initTask() throws Exception {
						producer.declareQueue();
					}
					
					@Override
					public void doTask() throws Exception {
						Message msg = new Message();
						
						msg.setBody(new byte[msgSize]); 
						msg = producer.produce(msg); 
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

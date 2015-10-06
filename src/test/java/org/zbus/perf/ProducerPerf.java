package org.zbus.perf;

import org.zbus.broker.Broker;
import org.zbus.kit.ConfigKit;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class ProducerPerf{
	
	public static void main(String[] args) throws Exception { 
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 32);
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyMQ");
		 
		Perf perf = new Perf(){
			@Override
			public MessageInvoker setupInvoker(Broker broker) throws Exception {
				Producer producer = new Producer(broker, mq);
				producer.createMQ();
				return producer;
			}
			
			@Override
			public void doInvoking(MessageInvoker invoker) throws Exception {
				Message msg = new Message();
				msg.setBody("hello world");
				invoker.invokeSync(msg, 10000);
			}
		};
		perf.serverAddress = serverAddress;
		perf.threadCount = threadCount;
		perf.loopCount = loopCount;
		
		perf.run();
	}
}

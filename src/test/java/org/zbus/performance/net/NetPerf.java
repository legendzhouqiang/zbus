package org.zbus.performance.net;

import org.zbus.broker.Broker;
import org.zbus.kit.ConfigKit;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.performance.Perf;

public class NetPerf{
	
	public static void main(String[] args) throws Exception { 
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:8080");
		final int threadCount = ConfigKit.option(args, "-c", 16); //并发线程数 
		final int selectorCount = ConfigKit.option(args, "-selector", 0);
		final int executorCount = ConfigKit.option(args, "-executor", 0);
		
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);   
		 
		Perf perf = new Perf(){
			@Override
			public MessageInvoker setupInvoker(Broker broker) { 
				return broker;
			}
			
			@Override
			public void doInvoking(MessageInvoker invoker) throws Exception {
				Message msg = new Message();
				msg.setCmd("/hello");
				msg.setBody("hello world");
			    invoker.invokeSync(msg, 10000); 
			}
		};
		perf.selectorCount = selectorCount;
		perf.executorCount = executorCount;
		perf.serverAddress = serverAddress;
		perf.threadCount = threadCount;
		perf.loopCount = loopCount;
		perf.logInterval = 5000;
		
		perf.run();
	}
}

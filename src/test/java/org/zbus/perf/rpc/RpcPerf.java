package org.zbus.perf.rpc;

import java.util.concurrent.atomic.AtomicLong;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.biz.Interface;
import org.zbus.rpc.mq.MqInvoker;


public class RpcPerf {
	static class Task extends Thread{
		final Logger log = Logger.getLogger(Task.class);
		Interface biz;
		int loopCount = 10000; 
		long startTime;
		AtomicLong counter;
		AtomicLong failCounter;
		@Override
		public void run() {  
			for(int i=0;i<loopCount;i++){ 
				try { 
					long count = counter.incrementAndGet();
					biz.getString("test");
					
					if(count%5000 == 0){
						long end = System.currentTimeMillis();
						String qps = String.format("%.2f", count*1000.0/(end-startTime));
						log.info("QPS: %s, Failed/Total=%d/%d(%.2f)",
								qps, failCounter.get(), counter.get(), 
								failCounter.get()*1.0/counter.get()*100);
					} 
				} catch (Exception e) { 
					failCounter.incrementAndGet();
					log.info(e.getMessage(), e);
					log.info("total failure %d of %d request", failCounter.get(), counter.get());
				}
			}
		}
	}
	public static void main(String[] args) throws Exception { 
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 1); 
		final int loopCount = ConfigKit.option(args, "-loop", 10000000);  
		final String mq = ConfigKit.option(args, "-mq", "MyRpc");
		final int selectorCount = ConfigKit.option(args, "-selector", 0);
		final int executorCount = ConfigKit.option(args, "-executor", 0);
		 
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress(serverAddress);
		brokerConfig.setMaxTotal(threadCount);
		brokerConfig.setMaxIdle(threadCount);  
		brokerConfig.setSelectorCount(selectorCount);
		brokerConfig.setExecutorCount(executorCount);
		
		final Broker broker = new SingleBroker(brokerConfig);
		 
		MessageInvoker invoker = new MqInvoker(broker, mq);
		//MessageInvoker invoker = broker; //DirectRpc时，直接用broker
		
		RpcFactory proxy = new RpcFactory(invoker);  
		 
		Interface biz = proxy.getService(Interface.class);
		
		AtomicLong counter = new AtomicLong(0);
		AtomicLong faileCounter = new AtomicLong(0);
		final long start = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount];
		for(int i=0;i<tasks.length;i++){
			tasks[i] = new Task();
			tasks[i].biz = biz;
			tasks[i].loopCount = loopCount;
			tasks[i].startTime = start;
			tasks[i].counter = counter;
			tasks[i].failCounter = faileCounter;
		}
		
		for(Task task : tasks){
			task.start();
		} 
		for(Task task : tasks){
			task.join();
		}
		
		System.out.println("===done===");
		broker.close(); 
	}
}

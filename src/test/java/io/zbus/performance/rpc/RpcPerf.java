package io.zbus.performance.rpc;

import java.util.concurrent.atomic.AtomicLong;

import io.zbus.examples.rpc.appdomain.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.broker.BrokerConfig;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.rpc.RpcFactory;
import io.zbus.rpc.mq.MqInvoker;
import io.zbus.util.ConfigUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;


public class RpcPerf {
	static class Task extends Thread{
		final Logger log = LoggerFactory.getLogger(Task.class);
		InterfaceExample biz;
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
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigUtil.option(args, "-c", 32); 
		final int loopCount = ConfigUtil.option(args, "-loop", 10000000);  
		final String mq = ConfigUtil.option(args, "-mq", "MyRpc"); 
		 
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setBrokerAddress(serverAddress);
		brokerConfig.setConnectionPoolMaxSize(threadCount); 
		
		final Broker broker = new ZbusBroker(brokerConfig);
		 
		MessageInvoker invoker = new MqInvoker(broker, mq);
		//MessageInvoker invoker = broker; //DirectRpc时，直接用broker
		
		RpcFactory proxy = new RpcFactory(invoker);  
		 
		InterfaceExample biz = proxy.getService(InterfaceExample.class);
		
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

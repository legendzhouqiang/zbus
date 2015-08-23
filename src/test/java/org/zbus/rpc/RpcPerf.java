package org.zbus.rpc;

import java.util.concurrent.atomic.AtomicLong;

import org.zbus.kit.ConfigKit;
import org.zbus.log.Logger;
import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.biz.Interface;
import org.zbus.rpc.broking.BrokingInvoker;

class Task extends Thread{
	private static final Logger log = Logger.getLogger(Task.class);
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
				biz.getOrder();
				
				if(count%1000 == 0){
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

public class RpcPerf {
	public static void main(String[] args) throws Exception { 
		final String brokerAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 60);
		final int loopCount = ConfigKit.option(args, "-loop", 10000);
		final String serviceName = ConfigKit.option(args, "-service", "MyRpc");
		final int timeout = ConfigKit.option(args, "-timeout", 10000);
		final boolean verbose = ConfigKit.option(args, "-verbose", false);
		
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setBrokerAddress(brokerAddress);
		brokerConfig.setMaxTotal(threadCount);
		brokerConfig.setMaxIdle(threadCount);  
		
		final Broker broker = new SingleBroker(brokerConfig);
		MessageInvoker invoker = new BrokingInvoker(broker, serviceName);
		
		RpcFactory proxy = new RpcFactory(invoker); 
		
		RpcConfig config = new RpcConfig(); 
		config.setTimeout(timeout);
		config.setVerbose(verbose);
		
		Interface biz = proxy.getService(Interface.class, config);
		
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

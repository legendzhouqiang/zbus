package org.zstacks.zbus.rpc;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;
import org.zstacks.zbus.rpc.biz.Interface;
import org.zstacks.znet.Helper;

class Task extends Thread{
	private static final Logger log = LoggerFactory.getLogger(Task.class);
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
				biz.getUserScore();
				
				if(count%1000 == 0){
					long end = System.currentTimeMillis();
					String qps = String.format("%.2f", count*1000.0/(end-startTime));
					log.info("QPS: {}, Failed/Total={}/{}({}%)",
							qps, failCounter.get(), counter.get(), 
							failCounter.get()*1.0/counter.get()*100);
				} 
			} catch (Exception e) { 
				failCounter.incrementAndGet();
				log.info(e.getMessage(), e);
				log.info("total failure {} of {} request", failCounter.get(), counter.get());
			}
		}
	}
}

public class RpcPerf {
	public static void main(String[] args) throws Exception { 
		final String brokerAddress = Helper.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = Helper.option(args, "-c", 100);
		final int loopCount = Helper.option(args, "-loop", 10000);
		final String serviceName = Helper.option(args, "-service", "MyRpc");
		final int timeout = Helper.option(args, "-timeout", 10000);
		
		SingleBrokerConfig config = new SingleBrokerConfig(); 
		config.setBrokerAddress(brokerAddress);
		config.setMaxTotal(threadCount);
		config.setMaxIdle(threadCount);  
		
		final Broker broker = new SingleBroker(config);
		
		RpcProxy proxy = new RpcProxy(broker); 
		String url = String.format("mq=%s&&timeout=%d", serviceName, timeout);
		
		Interface biz = proxy.getService(Interface.class, url);
		
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

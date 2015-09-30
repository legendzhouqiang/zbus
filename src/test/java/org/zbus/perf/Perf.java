package org.zbus.perf;

import java.util.concurrent.atomic.AtomicLong;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.kit.log.Logger;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class Perf {
	private static final Logger log = Logger.getLogger(Perf.class);
	public String serverAddress = "127.0.0.1:15555";
	public int threadCount = 16;
	public int loopCount = 1000000;
	public long startTime;
	public AtomicLong counter = new AtomicLong(0);
	public AtomicLong failCounter = new AtomicLong(0);
	
	private Broker broker;
	
	public MessageInvoker setupInvoker(Broker broker) throws Exception{
		return broker;
	}
	
	public void doInvoking(MessageInvoker invoker) throws Exception{
		Message msg = new Message();
		msg.setBody("hello world");
		invoker.invokeSync(msg, 10000);
	}
	
	
	
	public void run() throws Exception{
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress(serverAddress);
		brokerConfig.setMaxTotal(threadCount);
		brokerConfig.setMaxIdle(threadCount);  
		
		this.broker = new SingleBroker(brokerConfig); 
		 
		this.startTime = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount]; 
		for(int i=0;i<tasks.length;i++){ 
			tasks[i] = new Task();
		}
		
		for(Task task : tasks){
			task.start();
		} 
		for(Task task : tasks){
			task.join();
		}
		
		log.info("===done===");
		broker.close(); 
	}
	

	class Task extends Thread{ 
		MessageInvoker invoker;   
		public Task() throws Exception{
			invoker = setupInvoker(broker);
		} 
		@Override
		public void run() {  
			for(int i=0;i<loopCount;i++){ 
				try { 
					long count = counter.incrementAndGet(); 
					doInvoking(invoker);  
					if(count%1000 == 0){
						long end = System.currentTimeMillis();
						String qps = String.format("%.4f", count*1000.0/(end-startTime));
						log.info("QPS: %s, Failed/Total=%d/%d(%.4f)",
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
}

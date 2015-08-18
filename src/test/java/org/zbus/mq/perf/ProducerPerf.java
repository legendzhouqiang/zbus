package org.zbus.mq.perf;

import java.util.concurrent.atomic.AtomicLong;

import org.zbus.kit.ConfigKit;
import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.Producer;
import org.zbus.mq.SingleBroker;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;

public class ProducerPerf { 
	static class Task extends Thread{
		private final Producer producer;
		private final AtomicLong counter;
		private final long startTime;
		private final long N;
		public Task(Producer producer, AtomicLong counter, long startTime, long N) { 
			this.producer = producer;
			this.counter = counter;
			this.startTime = startTime;
			this.N = N;
		}
		@Override
		public void run() { 
			for(int i=0; i<N; i++){
				Message msg = new Message(); 
				msg.setBody("hello");
//				if((i+1)%1000 == 0){
//					try {
//						sleep(50);
//					} catch (InterruptedException e) { 
//					}
//				}
				try {
					producer.sendSync(msg, 10000);
					counter.incrementAndGet();
				} catch (Exception e) { 
					e.printStackTrace();
				}
				if(counter.get()%5000==0){
					double qps = counter.get()*1000.0/(System.currentTimeMillis()-startTime);
					System.out.format("QPS: %.2f\n", qps);
				}
			}
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		int selectorCount = ConfigKit.option(args, "-selector", 1);
		int executorCount = ConfigKit.option(args, "-executor", 128);
		final long N = ConfigKit.option(args, "-N", 1000000);
		final int threadCount =  ConfigKit.option(args, "-thread", 16);
		final String serverAddress = ConfigKit.option(args, "-s", "127.0.0.1:15555");
		
		Dispatcher dispatcher = new Dispatcher()
				.selectorCount(selectorCount)
				.executorCount(executorCount);
		
		dispatcher.start();
	 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress(serverAddress);
		config.setDispatcher(dispatcher);
		
		final Broker broker = new SingleBroker(config);
  
		final AtomicLong counter = new AtomicLong(0);
		
		Producer[] clients = new Producer[threadCount];
		for(int i=0;i<clients.length;i++){
			clients[i] = new Producer(broker, "MyMQ");
			clients[i].createMQ();
		}
		
		final long startTime = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount];
		for(int i=0; i<threadCount; i++){
			tasks[i] = new Task(clients[i], counter, startTime, N);
		}
		for(Task task : tasks){
			task.start();
		} 
		 
		
		//dispatcher.close();
		 
	}
}

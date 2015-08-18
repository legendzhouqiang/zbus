package org.zbus.net.perf;

import java.util.concurrent.atomic.AtomicLong;

import org.zbus.kit.ConfigKit;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.Message;

class Task extends Thread{
	private final MessageClient client;
	private final AtomicLong counter;
	private final long startTime;
	private final long N;
	public Task(MessageClient client, AtomicLong counter, long startTime, long N) {
		this.client = client;
		this.counter = counter;
		this.startTime = startTime;
		this.N = N;
	}
	@Override
	public void run() { 
		for(int i=0; i<N; i++){
			Message msg = new Message();
			msg.setCmd("hello");
			try {
				client.invokeSync(msg);
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

public class RemotingPerf {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		int selectorCount = ConfigKit.option(args, "-selector", 1);
		int executorCount = ConfigKit.option(args, "-executor", 128);
		final long N = ConfigKit.option(args, "-N", 1000000);
		final int threadCount =  ConfigKit.option(args, "-thread", 32);
		final String serverAddress = ConfigKit.option(args, "-s", "127.0.0.1:80");
		
		Dispatcher dispatcher = new Dispatcher()
				.selectorCount(selectorCount)
				.executorCount(executorCount);
		
		dispatcher.start();
	 
		final AtomicLong counter = new AtomicLong(0);
		
		MessageClient[] clients = new MessageClient[threadCount];
		for(int i=0;i<clients.length;i++){
			clients[i] = new MessageClient(serverAddress, dispatcher);
		}
		
		final long startTime = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount];
		for(int i=0; i<threadCount; i++){
			tasks[i] = new Task(clients[i], counter, startTime, N);
		}
		for(Task task : tasks){
			task.start();
		} 
		
		//4）释放链接资源与线程池相关资源
		//client.close();
		//dispatcher.close();
	} 
}

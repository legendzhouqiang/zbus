package org.zstacks.zbus.perf;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

class Task extends Thread{
	Broker broker;
	int count = 10000;
	String service = "MyService";
	
	@Override
	public void run() {
		Caller p = new Caller(broker, service);
		for(int i=0;i<count;i++){ 
			Message msg = new Message();
			msg.setBody("hello");
			try {
				p.invokeSync(msg, 2500); 
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
	}
}

public class ServicePerf {
	public static void main(String[] args) throws Exception { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		//config.setBrokerAddress("10.8.60.250:15555");
		config.setMaxTotal(1000);
		config.setMinIdle(128);
		final Broker broker = new SingleBroker(config);
		
		final int count = 1000; 
		final int threadCount = 64;
		final long start = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount];
		for(int i=0;i<tasks.length;i++){
			tasks[i] = new Task();
			tasks[i].broker = broker;
			tasks[i].count = count;
		}
		
		for(Task task : tasks){
			task.start();
		} 
		for(Task task : tasks){
			task.join();
		}
		
		
		long end = System.currentTimeMillis();
		System.out.println(count*threadCount*1000.0/(end-start));
		//broker.close();
		
	}
}

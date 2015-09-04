package org.zbus.mq.diskq;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;


public class BlockingQueuePerf {
	
	static class Task extends Thread{ 
		int loopCount = 10000; 
		long startTime;
		AtomicLong counter;
		BlockingQueue<byte[]> q;
		@Override
		public void run() { 
			for(int i=0;i<loopCount;i++){ 
				try {
					q.offer(new byte[10]);
					long count = counter.incrementAndGet();
					if(count%20000 == 0){
						long end = System.currentTimeMillis();
						System.out.format("QPS: %.2f\n", count*1000.0/(end-startTime));
					}
					
				} catch (Exception e) { 
					e.printStackTrace();
				}
				
			}
		}
	}
	public static void main(String[] args) throws Exception { 
		BlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>();
		final int loopCount = 10000000; 
		final int threadCount = 16;
		
		AtomicLong counter = new AtomicLong(0);
		final long start = System.currentTimeMillis();
		Task[] tasks = new Task[threadCount];
		for(int i=0;i<tasks.length;i++){
			tasks[i] = new Task(); 
			tasks[i].loopCount = loopCount;
			tasks[i].startTime = start;
			tasks[i].counter = counter;
			tasks[i].q = q;
		}
		
		for(Task task : tasks){
			task.start();
		} 
		for(Task task : tasks){
			task.join();
		}
		
		System.out.println("===done==="); 
	}
}

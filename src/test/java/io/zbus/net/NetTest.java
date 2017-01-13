package io.zbus.net;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NetTest {
	
	public static void main(String[] args) throws Exception { 
		
		ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 120, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>(1),
				new ThreadPoolExecutor.CallerRunsPolicy()); 
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					System.out.println("OK");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		for(int i=0;i<10;i++){
			executor.submit(task);
		}
		System.out.println("done");
		executor.shutdown();
	}
}

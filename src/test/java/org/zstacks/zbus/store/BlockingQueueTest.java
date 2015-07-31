package org.zstacks.zbus.store;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class BlockingQueueTest {

	public static void main(String[] args) { 
		BlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>(1000);
		
		long start = System.currentTimeMillis();
		final int N = 10000000;
		
		for(int i=0;i<N;i++){ 
			q.offer(new byte[10]);
		}
		
		long end = System.currentTimeMillis();
		System.out.println(N*1000.0/(end-start)); 
		 
	}

}

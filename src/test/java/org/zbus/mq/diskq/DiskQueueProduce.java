package org.zbus.mq.diskq;

import org.zbus.mq.disk.DiskQueue;
import org.zbus.mq.disk.DiskQueuePool;



public class DiskQueueProduce {

	public static void main(String[] args) { 
		String path = "mq"; 
		DiskQueuePool.init(path);
		DiskQueue q = DiskQueuePool.getDiskQueue("test");
		
		long start = System.currentTimeMillis();
		final int N = 10000000; 
		int count = 100; 
		for(int i=0;i<N;i++){   
			q.offer(new byte[count]);
		}
		
		long end = System.currentTimeMillis();
		System.out.format("QPS: %.2f\n", N*1000.0/(end-start));
		System.out.format("MPS: %.2fM/s\n", count*N*1000.0/(end-start)/1024/1024);
		
		DiskQueuePool.destory(); 
	}

}

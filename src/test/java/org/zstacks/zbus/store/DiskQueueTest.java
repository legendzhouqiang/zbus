package org.zstacks.zbus.store;



public class DiskQueueTest {

	public static void main(String[] args) { 
		String path = "C:\\DiskQueue"; 
		DiskQueuePool.init(path);
		DiskQueue q = DiskQueuePool.getDiskQueue("test");
		
		long start = System.currentTimeMillis();
		final int N = 1000000; 
		int size = 100; 
		for(int i=0;i<N;i++){   
			q.offer(new byte[size]);
		}
		
		long end = System.currentTimeMillis();
		System.out.format("QPS: %.2f\n", N*1000.0/(end-start));
		System.out.format("MPS: %.2fM/s\n", size*N*1000.0/(end-start)/1024/1024);
		
		DiskQueuePool.destory(); 
	}

}

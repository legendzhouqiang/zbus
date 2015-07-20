package org.zstacks.zbus.store;

import org.zstacks.zbus.store.MFQueuePool.MFQueue;

public class MFQTest {

	public static void main(String[] args) { 
		MFQueuePool.init("c:\\MFQ");
		MFQueue q = MFQueuePool.getFQueue("test");
		
		long start = System.currentTimeMillis();
		final int N = 10000000;
		
		for(int i=0;i<N;i++){ 
			//byte[] bs = q.poll();
			//System.out.println(new String(bs));
			q.offer(new byte[10]);
		}
		
		long end = System.currentTimeMillis();
		System.out.println(N*1000.0/(end-start));
		MFQueuePool.destory();
	}

}

package org.zbus.mq.diskq;

import org.zbus.mq.disk.DiskQueue;
import org.zbus.mq.disk.DiskQueuePool;



public class DiskQueueConsume {

	public static void main(String[] args) { 
		String path = "C:\\DiskQueue"; 
		DiskQueuePool.init(path);
		DiskQueue q = DiskQueuePool.getDiskQueue("test");
		
		long start = System.currentTimeMillis();
		int i = 0;
		int count = 100; 
		while(true){   
			byte[] data = q.poll();
			if(data == null) break;
			i++;
		}
		
		long end = System.currentTimeMillis();
		System.out.format("Total: %d\n", i);
		System.out.format("QPS: %.2f\n", i*1000.0/(end-start));
		System.out.format("MPS: %.2fM/s\n", count*i*1000.0/(end-start)/1024/1024);
		
		DiskQueuePool.release(); 
	}

}

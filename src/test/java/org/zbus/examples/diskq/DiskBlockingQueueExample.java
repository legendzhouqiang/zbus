package org.zbus.examples.diskq;

import org.zbus.mq.disk.DiskBlockingQueue;
import org.zbus.mq.disk.DiskQueue;
import org.zbus.mq.disk.DiskQueuePool;

public class DiskBlockingQueueExample {

	public static void main(String[] args) throws Exception {  
		//1) create a DiskQueue
		//1.1) DiskQueue is pooled, so create a pool instead
		DiskQueuePool pool = new DiskQueuePool("./test");
		//1.2) get it from pool
		DiskQueue support = pool.getDiskQueue("MyMQ");
		
		//2) decorate to a blocking queue
		DiskBlockingQueue diskq = new DiskBlockingQueue(support);
		
		byte[] data = diskq.take();
		System.out.println(data);
		
		//3) close the pool to release directory
		pool.close(); 
	}

}

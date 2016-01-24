package org.zbus.examples.diskq;

import org.zbus.mq.disk.DiskQueue;
import org.zbus.mq.disk.DiskQueuePool;

public class DiskQueueExample {

	public static void main(String[] args) throws Exception {  
		//1) disk queue is pooled, you can only get it from pool
		// all queues reside in same directory share a same pool
		DiskQueuePool pool = new DiskQueuePool("./test");
		//2) get it from pool
		DiskQueue q = pool.getDiskQueue("MyMQ");
		q.offer("hello diskq".getBytes());
		
		//3) close the pool to release
		pool.close();
	}

}

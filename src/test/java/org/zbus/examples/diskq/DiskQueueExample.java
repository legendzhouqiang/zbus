package org.zbus.examples.diskq;

import org.zbus.mq.disk.DiskQueue;
import org.zbus.mq.disk.DiskQueuePool;

/**
 * Run this example for as many times as you can, the queue data is persisted in the 
 * corresponding disk queue, check it in the file directory you provided.
 * 
 * @author rushmore (洪磊明)
 *
 */
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

package org.zbus.mq.diskq;

import java.util.AbstractQueue;

import org.zbus.mq.disk.DiskQueuePool;
import org.zbus.mq.disk.MessageDiskQueue;
import org.zbus.net.http.Message;

public class MessageDiskQueuePerf {

	public static void main(String[] args) { 
		String path = "mq"; 
		DiskQueuePool.init(path); 
		
		AbstractQueue<Message> q = null;
		q = new MessageDiskQueue("test");  
		
		long start = System.currentTimeMillis();
		
		final int N = 10000000; 
		int count = 10; 
		byte[] data = new byte[count];
		Message msg = new Message();
		msg.setBody(data);
		
		for(int i=0;i<N;i++){  
			q.offer(msg);
		}
		
		long end = System.currentTimeMillis();
		System.out.format("QPS: %.2f\n", N*1000.0/(end-start));
		System.out.format("MPS: %.2fM/s\n", count*N*1000.0/(end-start)/1024/1024);
		
		DiskQueuePool.release(); 
	}

}

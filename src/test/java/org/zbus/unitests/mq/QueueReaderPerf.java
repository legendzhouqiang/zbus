package org.zbus.unitests.mq;

import java.io.File;

import org.zbus.mq.ConsumeGroup;
import org.zbus.mq.disk.Index;
import org.zbus.mq.server.DiskQueue;

public class QueueReaderPerf {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));   
		
		DiskQueue queue = new DiskQueue(index);
		ConsumeGroup group = new ConsumeGroup("ConsumeGroup2");
		queue.declareConsumeGroup(group);  
		
		System.out.println(queue.remaining("ConsumeGroup2"));
		 
	} 
}

package io.zbus.performance.disk;

import java.io.File;

import io.zbus.mq.ConsumerGroup;
import io.zbus.mq.disk.Index;
import io.zbus.mq.server.DiskQueue;

public class QueueReaderPerf {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));   
		
		DiskQueue queue = new DiskQueue(index);
		ConsumerGroup group = new ConsumerGroup("ConsumeGroup2");
		queue.declareConsumerGroup(group);  
		
		System.out.println(queue.remaining("ConsumeGroup2"));
		 
	} 
}

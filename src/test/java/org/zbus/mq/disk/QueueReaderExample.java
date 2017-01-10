package org.zbus.mq.disk;

import java.io.File;

import org.zbus.mq.disk.DiskMessage;
import org.zbus.mq.disk.Index;
import org.zbus.mq.disk.QueueReader;

public class QueueReaderExample {

	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));
		QueueReader reader = new QueueReader(index, "ConsumeGroup1");
		while(true){
			DiskMessage message = reader.read();
			if(message == null) break;
		}
		
		reader.close();
	}
}

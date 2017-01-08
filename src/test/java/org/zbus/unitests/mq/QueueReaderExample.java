package org.zbus.unitests.mq;

import java.io.File;

import org.zbus.mq.disk.Index;
import org.zbus.mq.disk.QueueReader;

public class QueueReaderExample {

	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));
		QueueReader reader = new QueueReader(index, "ConsumeGroup2");
		reader.read();
		
		reader.close();
	}
}

package org.zbus.unitests.mq;

import java.io.File;

import org.zbus.mq.disk.DiskMessage;
import org.zbus.mq.disk.Index;
import org.zbus.mq.disk.QueueReader;

public class QueueReaderPerf {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));  
		
		QueueReader reader = new QueueReader(index, "ConsumeGroup2"); 
		reader.setFilterTag("abc.*"); 
		
		long count = 0; 
		while(true){
			DiskMessage data = reader.read();
			if(data == null) break;
			count++; 
			System.out.println(data.bytesScanned +  ": " + count);
			
		} 
		System.out.println(count);
		
		reader.close();
	} 
}

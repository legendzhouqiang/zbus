package org.zbus.unitests.mq;

import java.io.File;

import org.zbus.mq.disk.DiskMessage;
import org.zbus.mq.disk.Index;
import org.zbus.mq.disk.QueueReader;
import org.zbus.mq.disk.QueueWriter;

public class QueueWriterTest {
	
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/StringQueue")); 
		QueueWriter w = new QueueWriter(index);
		
		for(int i=0; i<1;i++){
			DiskMessage message = new DiskMessage();
			message.body = new String("hello"+i).getBytes();
			w.write(message);
		}
		
		QueueReader r = new QueueReader(index, "MyGroup4");
		while(true){
			DiskMessage data = r.read();
			if(data == null) break;
			System.out.println(new String(data.body));
		}
		
		r.close();
		index.close();
	}
	
}

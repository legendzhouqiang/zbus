package org.zbus.unitests.mq;

import java.io.File;

import org.zbus.mq.disk.Index;

public class IndexTest {
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/AQueue2")); 
		 
		index.setExt(1, "hello");
		System.out.println(index.getExt(1));
		
		index.close();
	}
	
}

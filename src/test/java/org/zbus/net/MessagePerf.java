package org.zbus.net;

import org.zbus.net.http.Message;
import org.zbus.net.http.MessageCodec;


public class MessagePerf {

	public static void main(String[] args) { 
		
		final int N = 2000000;   
		
		Message msg = new Message(); 
		msg.setMq("xx");
		msg.setId("123456"); 
		msg.setHead("key1", "value1");
		msg.setHead("key2", "value2");
		msg.setHead("key3", "value3");
		msg.setHead("key4", "value4");
		msg.setHead("key5", "value5");
		msg.setBody("hello world");
		 
		long start = System.currentTimeMillis();
		
		MessageCodec codec = new MessageCodec();
		for(int i=0; i<N; i++){   
			codec.encode(msg);
		}
		
		long end = System.currentTimeMillis();
		System.out.println(N*1000.0/(end-start));

	}
}

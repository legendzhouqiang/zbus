package org.zbus.examples.net;

import org.zbus.net.http.Message;

public class MessageTest {

	public static void main(String[] args) throws Exception {
		Message msg = new Message();
		msg.setUrl("/test/xx?key=value");
		msg.setBody("hello"); 
		
		System.out.println(msg);
		 
		System.out.println(msg.asResponse(200));
		
	}

}

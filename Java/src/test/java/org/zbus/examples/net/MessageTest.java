package org.zbus.examples.net;

import org.zbus.net.http.Message;

public class MessageTest {

	public static void main(String[] args) throws Exception {
		Message msg = new Message();
		msg.setUrl("/test/xx");
		String key, value;
		key = "key";
		value = "中文";
		msg.setRequestParam(key,value);
		msg.setBody("hello"); 
		
		System.out.println(msg);
		 
	}

}

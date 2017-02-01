package io.zbus.net;

import io.zbus.mq.Message;

public class MessageTest {
	
	public static void main(String[] args) throws Exception { 
		Message message = new Message();
		message.setBody("中文");
		String content = "中文";
		System.out.println(content.length());
		System.out.println(message.getBody().length);
	}
	
}

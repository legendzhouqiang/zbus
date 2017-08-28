package org.zbus.examples.service;

import java.io.IOException;

import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;

public class UrlServiceExample {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException{ 
		
		UrlService svc = new UrlService("127.0.0.1:15555", "test");
		
		//1) filter(url regex)
		//2) url regex 
		svc.url("hello", new MessageProcessor() {
			@Override
			public Message process(Message req) {
				Message res = new Message();
				res.setStatus(200);
				res.setBody("hello world " + System.currentTimeMillis());
				return res;
			}
		});

		svc.start();  
	}
}

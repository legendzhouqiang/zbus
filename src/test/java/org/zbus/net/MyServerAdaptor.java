package org.zbus.net;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;

public class MyServerAdaptor extends MessageAdaptor{ 
	public MyServerAdaptor(){  
		uri("/hello", new MessageProcessor() {
			
			@Override
			public Message process(Message request) { 
				request.setBody("hello");
				return request;
			}
		});
	} 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Dispatcher dispatcher = new Dispatcher();  
		Server server = new Server(dispatcher, new MyServerAdaptor(), 80);
    	server.start(); 
	}
}

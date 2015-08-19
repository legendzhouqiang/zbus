package org.zbus.net;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.UriHandler;

public class MyServerAdaptor extends MessageAdaptor{ 
	public MyServerAdaptor(){   
		uri("/hello", new UriHandler() {  
			public Message process(Message req) {  
				Message res = new Message(); 
				res.setBody("hello world");
				return res;
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

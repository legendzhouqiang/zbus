package org.zbus.net;

import java.io.IOException;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
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

	public static void main(String[] args) throws Exception {   
		final Dispatcher dispatcher = new Dispatcher();  
		IoAdaptor ioAdaptor = new MyServerAdaptor();
		
		final Server server = new Server(dispatcher);
		//相同的业务处理逻辑可以便捷的侦听多个地址
		server.registerAdaptor(80, ioAdaptor);
		server.registerAdaptor(8080, ioAdaptor);
		
    	server.start(); 
    	
    	Runtime.getRuntime().addShutdownHook(new Thread(){
    		@Override
    		public void run() {  
    	    	try {
    	    		server.close();
					dispatcher.close();
				} catch (IOException e) { 
				}
    		}
    	}); 
	}
}

package org.zbus.net;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;


public class MyServerAdaptor extends MessageAdaptor{ 
	public MyServerAdaptor(){  
		uri("/hello", new MessageProcessor() { 
			@Override
			public Message process(Message request) { 
				request.setResponseStatus(200);
				request.setBody("hello");
				return request;
			}
		});  
	} 

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		final SelectorGroup dispatcher = new SelectorGroup();   
		final Server server = new Server(dispatcher);
		
		//相同的业务处理逻辑可以便捷的侦听多个地址
		IoAdaptor ioAdaptor = new MyServerAdaptor();
		server.registerAdaptor(80, ioAdaptor);
		server.registerAdaptor(15555, ioAdaptor);
		
    	server.start(); 
	}
}

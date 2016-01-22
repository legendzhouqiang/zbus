package org.zbus.performance.net;

import org.zbus.kit.ConfigKit;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;


public class NetServer extends MessageAdaptor{ 
	public NetServer(){  
		uri("/hello", new MessageProcessor() { 
			@Override
			public Message process(Message request) {
				Message res = new Message();
				res.setResponseStatus(200);
				res.setBody("hello");
				return res;
			}
		});  
	} 

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		final int selectCount = ConfigKit.option(args, "-selector", 0);  
		final int port = ConfigKit.option(args, "-p", 8080);
		
		final Dispatcher dispatcher = new Dispatcher(); 
		dispatcher.selectorCount(selectCount); 
		final Server server = new Server(dispatcher);
		 
		IoAdaptor ioAdaptor = new NetServer(); 
		server.registerAdaptor(port, ioAdaptor);
		
    	server.start(); 
	}
}

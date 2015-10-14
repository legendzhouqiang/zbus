package org.zbus.perf.net;

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
				request.setResponseStatus(200);
				request.setBody("hello");
				return request;
			}
		});  
	} 

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		final int selectCount = ConfigKit.option(args, "-selector", 0);  
		final int executorCount = ConfigKit.option(args, "-executor", 0);
		final int port = ConfigKit.option(args, "-p", 8080);
		
		final Dispatcher dispatcher = new Dispatcher(); 
		dispatcher.selectorCount(selectCount);
		dispatcher.executorCount(executorCount);
		final Server server = new Server(dispatcher);
		 
		IoAdaptor ioAdaptor = new NetServer(); 
		server.registerAdaptor(port, ioAdaptor);
		
    	server.start(); 
	}
}

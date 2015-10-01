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
		final int selectCount = ConfigKit.option(args, "-selector", 1);
		final int executorCount = ConfigKit.option(args, "-thread", 48);
		final Dispatcher dispatcher = new Dispatcher(); 
		dispatcher.selectorCount(selectCount);
		dispatcher.executorCount(executorCount);
		final Server server = new Server(dispatcher);
		
		//相同的业务处理逻辑可以便捷的侦听多个地址
		IoAdaptor ioAdaptor = new NetServer(); 
		server.registerAdaptor(8080, ioAdaptor);
		
    	server.start(); 
	}
}

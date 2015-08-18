package org.zbus.net;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.MessageHandler;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.Message;

public class MyServer extends MessageAdaptor{
	
	public MyServer(){  
		this.registerHandler("hello", new MessageHandler() {
			public void handle(Message msg, Session sess) throws IOException { 
				msg.setStatus(200);   
				msg.setBody("hello world");
				sess.write(msg);
			}
		});
	}
	

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		int selectorCount = ConfigKit.option(args, "-selector", 1);
		int executorCount = ConfigKit.option(args, "-executor", 128);
		
		Dispatcher dispatcher = new Dispatcher()
			.selectorCount(selectorCount)   //Selector线程数配置
			.executorCount(executorCount); //Message后台处理线程数配置
		
		IoAdaptor ioAdaptor = new MyServer();
		Server server = new Server(dispatcher, ioAdaptor, 80);
    	server.start(); 
	}
}

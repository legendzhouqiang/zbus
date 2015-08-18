package org.zbus.proxy;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageHandler;

public class TargetServer extends MessageAdaptor { 
	
	public TargetServer(){
		registerHandler("hello", new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				msg.setStatus(200);
				msg.setBody(""+System.currentTimeMillis());
				sess.write(msg);
			}
		});
	}  
	
	public static void main(String[] args) throws Exception { 
		Dispatcher dispatcher = new Dispatcher(); 
		TargetServer ioAdaptor = new TargetServer();
		
		@SuppressWarnings("resource")
		Server server = new Server(dispatcher, ioAdaptor, 8080);
		server.start(); 
	}

}

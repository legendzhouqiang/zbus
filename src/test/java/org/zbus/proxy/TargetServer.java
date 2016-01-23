package org.zbus.proxy;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;

public class TargetServer extends MessageAdaptor { 
	
	public TargetServer(){
		cmd("hello", new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				msg.setResponseStatus(200);
				msg.setBody(""+System.currentTimeMillis());
				sess.write(msg);
			}
		});
	}  
	
	public static void main(String[] args) throws Exception { 
		SelectorGroup dispatcher = new SelectorGroup(); 
		TargetServer ioAdaptor = new TargetServer();
		
		@SuppressWarnings("resource")
		Server server = new Server(dispatcher, ioAdaptor, 15555);
		server.start(); 
	}

}

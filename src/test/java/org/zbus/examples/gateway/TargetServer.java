package org.zbus.examples.gateway;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageAdaptor;

public class TargetServer {
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException { 
		SelectorGroup group = new SelectorGroup(); 
		Server server = new Server(group); 
		server.start(8080, new MessageAdaptor(){ 
			@Override
			public void onMessage(Object obj, Session sess) throws IOException {
				Message msg = (Message)obj;
				
				msg.setResponseStatus(200);
				msg.setBody(""+System.currentTimeMillis());
				
				sess.write(msg);
			}
		});  
	} 
}

package org.zbus.net.codec;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;

public class StringServer extends IoAdaptor{   
	public StringServer(){
		codec(new StringCodec());
	}  
	
    public void onMessage(Object obj, Session sess) throws IOException {  
    	System.out.println("recv: " + obj);
    	sess.write(obj);
    }  
    
    
    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Dispatcher dispatcher = new Dispatcher();
		
		StringServer ioAdaptor = new StringServer();
		
		Server server = new Server(dispatcher, ioAdaptor, 8080);
    	server.start();
	} 
}


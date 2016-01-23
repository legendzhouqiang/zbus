package org.zbus.examples.net.codec;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;

/**
 * String adaptor, application example, we only care about the message handling.
 * Simple logic: when message arrive, print to console and write back to client
 * 
 * @author rushmore (洪磊明)
 * @version 6.3.0
 *
 */
public class StringAdaptor extends IoAdaptor{   
	public StringAdaptor(){
		codec(new StringCodec());
	}  
    public void onMessage(Object obj, Session sess) throws IOException {  
    	//here message should be a string object
    	
    	System.out.println("recv: " + obj);
    	sess.write(obj);
    }   
    
    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
    	//1) create a SelectorGroup, just like EventLoopGroup in netty
		SelectorGroup group = new SelectorGroup();  
		//2) create a server to run
		Server server = new Server(group); 
		//3) register the adaptor defined above with server port, it can be multiple by the way.
		server.registerAdaptor(8080, new StringAdaptor()); 
		//4) simple? let's get it started
    	server.start();
	} 
}


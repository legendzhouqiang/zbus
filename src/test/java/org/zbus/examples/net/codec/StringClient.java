package org.zbus.examples.net.codec;

import java.io.IOException;

import org.zbus.net.Client;
import org.zbus.net.Client.MsgHandler;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;

/**
 * StringClient example, just extend Client
 * @author rushmore (洪磊明)
 *
 */
public class StringClient{
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		//1) create a dispatcher
		final Dispatcher dispatcher = new Dispatcher();  
		//2) create a client, lazy connection if needed
		Client<String, String> client = new Client<String, String>("127.0.0.1:8080", dispatcher);
		//3) set codec of message
		client.codec(new StringCodec()); 
		
		client.onMessage(new MsgHandler<String>() { 
			@Override
			public void handle(String msg, Session sess) throws IOException {
				System.out.println(msg); 
			}
		}); 
		
		//just try to ping pong 
		while(true){ 
			try{
				client.send("helloxxxxxxxxxxxxxyyyyy"); 
			}catch(Exception e){
				e.printStackTrace();
			}
			Thread.sleep(1000);
		} 
	}
}

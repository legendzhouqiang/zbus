package org.zbus.net.codec;

import java.io.IOException;

import org.zbus.net.Client;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;


public class StringClient extends Client<String, String> { 
	
	public StringClient(String address, Dispatcher dispatcher){
		super(address, dispatcher);
		codec(new StringCodec());
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		final Dispatcher dispatcher = new Dispatcher();  
		 
		final StringClient client = new StringClient("127.0.0.1:8080", dispatcher);

		client.onMessage(new MsgHandler<String>() {
			@Override
			public void handle(String msg, Session sess) throws IOException { 
				System.out.println(msg); 
			}
		});
		
		client.onError(new ErrorHandler() { 
			@Override
			public void onError(IOException e, Session sess) throws IOException {
				System.err.println(e);
			}
		});
		
		
		while(true){ 
			try{
				client.send("helloxxxxxxxxxxxxxyyyyy"); 
			}catch(Exception e){
				//e.printStackTrace();
			}
			Thread.sleep(1000);
		} 
	}
}

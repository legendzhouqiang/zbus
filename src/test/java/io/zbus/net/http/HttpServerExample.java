package io.zbus.net.http;

import io.zbus.net.Server;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		HttpMessageAdaptor adaptor = new HttpMessageAdaptor();
		
		adaptor.url("/", (msg, sess) -> {   
			HttpMessage res = new HttpMessage();
			res.setStatus(200);
			
			res.setId(msg.getId()); //match the ID for response 
			res.setBody(""+System.currentTimeMillis());
			sess.write(res); 
		});   
		 
		Server server = new HttpWsServer();   
		server.start(80, adaptor);  
	} 
}

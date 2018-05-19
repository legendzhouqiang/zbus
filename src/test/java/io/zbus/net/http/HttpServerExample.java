package io.zbus.net.http;

import io.zbus.transport.Server;
import io.zbus.transport.http.HttpWsServer;
import io.zbus.transport.http.HttpMessage;
import io.zbus.transport.http.HttpWsServerAdaptor;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		HttpWsServerAdaptor adaptor = new HttpWsServerAdaptor();
		
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

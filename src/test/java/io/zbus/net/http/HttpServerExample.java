package io.zbus.net.http;

import io.zbus.transport.Server;
import io.zbus.transport.http.HttpWsServer;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageServerAdaptor;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		MessageServerAdaptor adaptor = new MessageServerAdaptor();
		
		adaptor.url("/", (msg, sess) -> {   
			Message res = new Message();
			res.setStatus(200);
			
			res.setId(msg.getId()); //match the ID for response 
			res.setBody(""+System.currentTimeMillis());
			sess.write(res); 
		});   
		 
		Server server = new HttpWsServer();   
		server.start(80, adaptor);  
	} 
}

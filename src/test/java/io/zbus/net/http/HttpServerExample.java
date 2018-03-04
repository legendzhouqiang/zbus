package io.zbus.net.http;

import java.io.IOException;

import io.zbus.net.SessionMessageHandler;
import io.zbus.net.Session; 

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) {    
		HttpMsgAdaptor adaptor = new HttpMsgAdaptor();
		
		adaptor.url("/", new SessionMessageHandler<HttpMsg>() { 
			@Override
			public void handle(HttpMsg msg, Session session) throws IOException {  
				//System.out.println(msg);
				HttpMsg res = new HttpMsg();
				res.setStatus(200);
				
				res.setId(msg.getId()); //match the ID for response 
				res.setBody(""+System.currentTimeMillis());
				session.write(res);
			}
		});  
		 
		HttpServer server = new HttpServer();   
		server.start(80, adaptor);  
	} 
}

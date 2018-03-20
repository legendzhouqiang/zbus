package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		HttpMsgAdaptor adaptor = new HttpMsgAdaptor();
		
		adaptor.url("/", (msg, sess) -> {   
			HttpMsg res = new HttpMsg();
			res.setStatus(200);
			
			res.setId(msg.getId()); //match the ID for response 
			res.setBody(""+System.currentTimeMillis());
			sess.write(res); 
		});  
		
		EventLoop loop = new EventLoop(); 
		HttpServer server = new HttpServer(loop);   
		server.start(80, adaptor);  
	} 
}

package io.zbus.net.http;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.net.EventLoop;
import io.zbus.net.MessageHandler;
import io.zbus.net.Session;
import io.zbus.net.http.Message;
import io.zbus.net.http.MessageAdaptor;
import io.zbus.net.http.HttpServer; 

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) {   
		
		MessageAdaptor adaptor = new MessageAdaptor();
		
		adaptor.url("/", new MessageHandler<Message>() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {  
				//System.out.println(msg);
				Message res = new Message();
				res.setId(msg.getId()); //match the ID for response
				res.setStatus(200);
				res.setBody(JsonKit.toJSONString("xxxyzz"));
				session.write(res);
			}
		});  
		
		EventLoop loop = new EventLoop();
		loop.setIdleTimeInSeconds(30); 
		HttpServer server = new HttpServer(loop);   
		server.start(80, adaptor);  
	} 
}

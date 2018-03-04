package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();  
		HttpClient client = new HttpClient("localhost:15555", loop); 
		
		client.onMessage = msg->{
			System.out.println(msg);
			//client.close();
			//loop.close();
		};  
		
	
		
		HttpMsg req = new HttpMsg(); 
		req.setBody("Http Body"); 
		client.sendMessage(req);
	} 
}

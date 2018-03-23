package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientSync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("http://localhost", loop);
		
		HttpMessage req = new HttpMessage();    
		req.setUrl("/index/html");
		client.request(req);    
		
		client.close();
		loop.close();
	}
}

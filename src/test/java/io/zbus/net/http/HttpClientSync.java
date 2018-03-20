package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientSync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("http://localhost", loop);
		
		for(int i=0;i<100;i++){ 
			HttpMsg req = new HttpMsg();    
			client.request(req);   
		}
		
		client.close();
		loop.close();
	}
}

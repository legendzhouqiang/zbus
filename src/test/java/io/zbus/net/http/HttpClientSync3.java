package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientSync3 {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("http://api.zb.com", loop);
		
		for(int i=0;i<100;i++){ 
			HttpMsg req = new HttpMsg();  
			req.setUrl("/data/v1/markets");   
			client.request(req);   
		}
		
		client.close();
		loop.close();
	}
}

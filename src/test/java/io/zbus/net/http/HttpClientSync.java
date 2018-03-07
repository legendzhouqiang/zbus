package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientSync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("https://api.binance.com", loop);
		
		for(int i=0;i<10;i++){ 
			HttpMsg req = new HttpMsg();  
			req.setUrl("/api/v1/exchangeInfo");  
			
			client.request(req);   
		}
		
		client.close();
		loop.close();
	}
}

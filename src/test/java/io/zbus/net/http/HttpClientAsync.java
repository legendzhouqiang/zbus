package io.zbus.net.http;

import java.io.IOException;

import io.zbus.net.EventLoop;

public class HttpClientAsync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		HttpClient client = new HttpClient("https://api.binance.com", loop);
		HttpMsg req = new HttpMsg();  
		req.setUrl("/api/v1/exchangeInfo");   
		 
		client.request(req, resp->{ 
			try { 
				client.close(); 
				loop.close();
			} catch (IOException e) {  } 
		}, e-> { 
			client.close(); 
			loop.close();
		});  
	}
}

package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientExample { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("https://api.binance.com", loop);

		HttpMsg req = new HttpMsg();  
		req.setUrl("/api/v1/exchangeInfo");  
		
		client.request(req, resp->{
			System.out.println(resp);
		}, e->{
			e.printStackTrace();
		});  
	}
}

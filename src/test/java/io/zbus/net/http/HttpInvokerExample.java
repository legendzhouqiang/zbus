package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpInvokerExample {  
	
	public static void main(String[] args) throws Exception, InterruptedException {    
		EventLoop loop = new EventLoop();
		
		HttpInvoker http = new HttpInvoker("https://api.binance.com", loop);
		
		HttpMsg req = new HttpMsg();  
		req.setUrl("/api/v1/exchangeInfo");    
		Object resp = http.json(req, 10000);
		
		System.out.println(resp);
		
		loop.close();
	}
}

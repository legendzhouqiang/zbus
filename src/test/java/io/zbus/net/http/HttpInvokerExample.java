package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpInvokerExample {  
	
	public static void main(String[] args) throws Exception, InterruptedException {    
		EventLoop loop = new EventLoop();
		
		HttpInvoker http = new HttpInvoker(loop);
		
		HttpMsg req = new HttpMsg();  
		req.setUrl("https://api.binance.com/api/v1/exchangeInfo");    
		Object resp = http.json(req);
		
		System.out.println(resp);
		
		loop.close();
	}
}

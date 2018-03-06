package io.zbus.net.http;

public class HttpInvokerExample {  
	
	public static void main(String[] args) throws Exception, InterruptedException {    
		HttpMsg req = new HttpMsg();  
		req.setUrl("https://api.binance.com/api/v1/exchangeInfo");   
		
		Object resp = HttpInvoker.json(req, 10000);
		
		System.out.println(resp);
	}
}

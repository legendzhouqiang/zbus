package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientSync2 {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop(); 
		
		
		for(int i=0;i<100;i++){ 
			HttpClient client = new HttpClient("http://localhost/", loop);
			HttpMessage req = new HttpMessage();    
			client.request(req);   
			client.close();
		} 
		
		
		loop.close();
	}
}

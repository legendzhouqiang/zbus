package io.zbus.net.ssl;

import io.zbus.net.EventLoop;
import io.zbus.net.http.HttpClient; 
public class SslClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();   
		
		
		HttpClient client = new HttpClient("localhost:80", loop);
 
		
		client.close();
		loop.close();
	} 
}

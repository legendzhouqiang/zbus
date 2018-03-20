package io.zbus.net.http;

import java.io.IOException;

import io.zbus.net.EventLoop;

public class HttpClientAsync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("http://localhost", loop);
		HttpMsg req = new HttpMsg();   
		 
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

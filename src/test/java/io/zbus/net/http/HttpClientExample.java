package io.zbus.net.http;

import io.zbus.net.EventLoop;
import io.zbus.net.http.Message;
import io.zbus.net.http.HttpClient;

public class HttpClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();  
		HttpClient client = new HttpClient("localhost", loop);

		Message req = new Message(); 
		req.setBody("中文");
		client.sendMessage(req);
		client.messageHandler = (msg, session)->{
			System.out.println(msg);
			client.close();
			loop.close();
		}; 
	} 
}

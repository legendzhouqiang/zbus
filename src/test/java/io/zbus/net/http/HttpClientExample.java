package io.zbus.net.http;

import io.zbus.net.EventLoop;

public class HttpClientExample {

	public static void main(String[] args) throws Exception, InterruptedException {
		EventLoop loop = new EventLoop();
		HttpClient client = new HttpClient("localhost:15555", loop);

		HttpMsg req = new HttpMsg();
		req.setBody("hello world");
		client.requestAsync(req, res -> {
			System.out.println(res);
			client.close();
			loop.close();
		}, e -> {
			e.printStackTrace();
			client.close();
			loop.close();
		}); 
	}
}

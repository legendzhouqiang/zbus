package io.zbus.net.http;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.net.EventLoop;
import io.zbus.rpc.Request;

public class HttpClientAsync {   
	
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();
		
		HttpClient client = new HttpClient("http://localhost", loop);
		HttpMessage reqMsg = new HttpMessage();   
		
		Request req = new Request();
		req.command = "echo";
		req.params.add("hi");
		req.id = StrKit.uuid();
		 
		reqMsg.setBody(JsonKit.toJSONString(req));  
		
		client.request(reqMsg, resp->{ 
			System.out.println(resp);
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

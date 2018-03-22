package io.zbus.net.ws;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;
import io.zbus.rpc.Request;

public class WebSocketExample {
	 
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "ws://localhost/";
		
		WebsocketClient ws = new WebsocketClient(address, loop);
		
		ws.onMessage = msg->{
			System.out.println(msg);
			try {
				ws.close();
				loop.close();
			} catch (IOException e) { 
				e.printStackTrace();
			}
		};  
		
		ws.onOpen = ()-> {
			Request req = new Request();
			req.command = "echo";
			req.params.add("hi");
			req.id = StrKit.uuid();
			 
			ws.sendMessage(JsonKit.toJSONString(req));  
		};
		 
		ws.connect();  
	}
}

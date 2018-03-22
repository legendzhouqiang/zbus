package io.zbus.net.ws;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.net.EventLoop;
import io.zbus.net.http.ws.WebsocketClient;
import io.zbus.rpc.Request;

public class WebSocketExample {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "ws://localhost/";
		
		WebsocketClient ws = new WebsocketClient(address, loop);
		
		ws.onMessage = msg->{
			System.out.println(msg);
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

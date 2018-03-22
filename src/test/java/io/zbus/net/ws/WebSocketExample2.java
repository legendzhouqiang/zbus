package io.zbus.net.ws;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;

public class WebSocketExample2 {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "wss://api.zb.com:9999/websocket";
		
		WebsocketClient ws = new WebsocketClient(address, loop);
		
		ws.onMessage = msg->{
			System.out.println(msg);
		};  
		
		ws.onOpen = ()-> {
			Map<String, String> req = new HashMap<>();
			req.put("event", "addChannel");
			req.put("channel", "ltcbtc_ticker");
			 
			ws.sendMessage(JsonKit.toJSONString(req)); 
		};
		 
		ws.connect();
	}
}

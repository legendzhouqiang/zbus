package io.zbus.net.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.zbus.net.EventLoop;

public class WebSocketExample2 {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		EventLoop loop = new EventLoop();
		String address = "wss://api.zb.com:9999/websocket";
		
		WebSocket ws = new WebSocket(address, loop);
		
		ws.onMessage = msg->{
			System.out.println(msg);
		};  
		
		ws.onOpen = ()-> {
			JSONObject json = new JSONObject();
			json.put("event", "addChannel");
			json.put("channel", "ltcbtc_trades");
			 
			ws.sendMessage(JSON.toJSONString(json)); 
		};
		 
		ws.connect();  
	} 
}

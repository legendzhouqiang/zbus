package io.zbus.net.ws;

import io.zbus.net.EventLoop;

public class WebSocketExample {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		EventLoop loop = new EventLoop();
		String address = "wss://stream.binance.com:9443/ws/btcusdt@aggTrade";
		
		WebSocket ws = new WebSocket(address, loop);
		
		ws.onMessage = msg->{
			System.out.println(new String(msg));
		};
		
		ws.connect(); 
	} 
}

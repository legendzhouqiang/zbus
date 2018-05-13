package io.zbus.net.ws;

import java.io.IOException;

import io.zbus.net.WebsocketClient;

public class WebSocketExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		String address = "wss://stream.binance.com:9443/stream?streams=hsrbtc@depth/btcusdt@depth";

		WebsocketClient ws = new WebsocketClient(address);

		ws.onText = data -> {
			 System.out.println(data);
		};  
		
		ws.onBinary = data ->{
			System.out.println(data.remaining());
		};
		 
		ws.connect(); 
		new Thread(()->{
			while(true) {
				if(System.currentTimeMillis() - ws.lastActiveTime > 10_000) {
					try {
						ws.close();
						ws.connect();
					} catch (IOException e) { 
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
			}
		}).start();
	}
}

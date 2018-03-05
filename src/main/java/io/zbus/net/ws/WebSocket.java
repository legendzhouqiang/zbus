package io.zbus.net.ws;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.Client;
import io.zbus.net.EventLoop;
import io.zbus.net.http.HttpMsg;
import io.zbus.net.http.HttpMsgCodec;

public class WebSocket extends Client<HttpMsg, HttpMsg> {

	public WebSocket(String address, final EventLoop loop) {
		super(address, loop);
		codec(p -> {
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new HttpMsgCodec());
		});

		onClose = null; // Disable auto reconnect
		onError = null;
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		EventLoop loop = new EventLoop();
		String address = "wss://stream.binance.com:9443/ws/btcusdt@aggTrade";
		
		WebSocket ws = new WebSocket(address, loop);
		
		ws.onMessage = msg->{
			
		};
		
		ws.connect(); 
	} 
}

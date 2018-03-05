package io.zbus.net.ws;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.Client;
import io.zbus.net.EventLoop;

public class WebSocket extends Client<byte[], byte[]> {

	public WebSocket(String address, final EventLoop loop) {
		super(address, loop); 
		
		codec(p -> {
			p.clear();
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new WebSocketCodec(uri));
		}); 
	}
}

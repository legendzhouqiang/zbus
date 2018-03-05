package io.zbus.net.ws;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.zbus.net.Client;
import io.zbus.net.EventLoop;

public class WebSocket extends Client<byte[], byte[]> {

	public WebSocket(String address, final EventLoop loop) {
		super(address, loop);
		
	    WebSocketClientHandshaker hankshaker = WebSocketClientHandshakerFactory.newHandshaker(
                 uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
		
		codec(p -> {
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new WebSocketCodec(hankshaker));
		}); 
	}
}

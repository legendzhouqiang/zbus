package io.zbus.net.http;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.net.EventLoop;
import io.zbus.net.Server; 

public class HttpServer extends Server { 
	public HttpServer() {
		this(null);
	}

	public HttpServer(final EventLoop loop) {
		super(loop);  
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(getEventLoop().getPackageSizeLimit()));
			p.add(new HttpWsServerCodec()); 
		}); 
	} 
}

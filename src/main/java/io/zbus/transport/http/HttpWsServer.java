package io.zbus.transport.http;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.transport.Server; 

public class HttpWsServer extends Server { 
	public HttpWsServer() {
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(packageSizeLimit)); 
			p.add(new HttpWsServerCodec());  
		}); 
	} 
}

package io.zbus.mq;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.net.EventLoop;
import io.zbus.net.Server; 

public class MqServer extends Server { 
	public MqServer() {
		this(null);
	}

	public MqServer(final EventLoop loop) {
		super(loop);  
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(getEventLoop().getPackageSizeLimit()));  
		}); 
	} 
}

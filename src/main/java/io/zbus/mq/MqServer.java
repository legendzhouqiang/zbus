package io.zbus.mq;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.transport.Server;
import io.zbus.transport.http.HttpWsServerCodec; 

public class MqServer extends Server { 
	private MqServerAdaptor serverAdaptor;
	public MqServer() {
		this(null); 
	}

	public MqServer(final EventLoopGroup loop) {
		super(loop);  
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(packageSizeLimit));  
			p.add(new HttpWsServerCodec());
		}); 
		
		serverAdaptor = new MqServerAdaptor();
	} 
	
	public void start(int port) {
		this.start(port, serverAdaptor);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		MqServer server = new MqServer();
		server.start(15555);
	}
}

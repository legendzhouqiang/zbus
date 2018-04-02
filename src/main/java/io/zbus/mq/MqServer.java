package io.zbus.mq;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.net.EventLoop;
import io.zbus.net.Server;
import io.zbus.net.http.HttpWsServerCodec; 

public class MqServer extends Server { 
	private MqServerAdaptor serverAdaptor;
	public MqServer() {
		this(null); 
	}

	public MqServer(final EventLoop loop) {
		super(loop);  
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(getEventLoop().getPackageSizeLimit()));  
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
		server.start(80);
	}
}

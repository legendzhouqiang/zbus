package io.zbus.mq;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.transport.Server;
import io.zbus.transport.http.HttpWsServerCodec; 

public class MqServer extends Server { 
	private MqServerAdaptor serverAdaptor; 
	private final MqServerConfig config;
	
	public MqServer(MqServerConfig config) { 
		this.config = config;
		codec(p -> {
			p.add(new HttpServerCodec());
			p.add(new HttpObjectAggregator(config.packageSizeLimit));  
			p.add(new HttpWsServerCodec());
		}); 
		
		serverAdaptor = new MqServerAdaptor();
	} 
	
	public void start() {
		this.start(config.port, serverAdaptor);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		MqServerConfig config = new MqServerConfig();
		MqServer server = new MqServer(config);
		server.start();
	}
}

package io.zbus.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.transport.Server;
import io.zbus.transport.http.HttpWsServerCodec; 

public class MqServer extends Server {
	private static final Logger logger = LoggerFactory.getLogger(MqServer.class); 
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
		if(config.port == null) {
			logger.info("Networking disabled, zbus work as InProc mode");
			return;
		}
		this.start(config.port, serverAdaptor);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		MqServerConfig config = new MqServerConfig();
		config.port = 15555;
		MqServer server = new MqServer(config);
		server.start();
	}
}

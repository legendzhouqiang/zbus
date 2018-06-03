package io.zbus.mq;
 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.kit.ConfigKit;
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
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public void start() {
		if(config.port == null) {
			logger.info("Networking disabled, zbus work as InProc mode");
			return;
		}
		this.start(config.port, serverAdaptor);
	}
	 
	
	public static void main(String[] args) {
		String configFile = ConfigKit.option(args, "-conf", "conf/zbus.xml"); 
		
		final MqServer server;
		try{
			server = new MqServer(configFile);
			server.start(); 
		} catch (Exception e) { 
			e.printStackTrace(System.err);
			logger.warn(e.getMessage(), e); 
			return;
		} 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					logger.info("MqServer shutdown completed");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});    
	}
}

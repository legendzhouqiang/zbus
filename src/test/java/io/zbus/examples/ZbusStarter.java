package io.zbus.examples;

import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;

public class ZbusStarter {  
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555;  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}  
}

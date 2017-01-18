package io.zbus;

import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;

public class ZbusStartup {  
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555;   
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}  
}

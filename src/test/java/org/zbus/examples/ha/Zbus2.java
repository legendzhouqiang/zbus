package org.zbus.examples.ha;

import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;

public class Zbus2 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15556; 
		config.verbose = true; //print out message
		config.storePath = "/tmp/zbus/mq2";  
		config.trackServerList = "127.0.0.1:16666;127.0.0.1:16667";
		  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

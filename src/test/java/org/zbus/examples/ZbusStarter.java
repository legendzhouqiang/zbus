package org.zbus.examples;

import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;

public class ZbusStarter {

	/**
	 * Start zbus in embedded mode, simple?
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555; 
		config.verbose = true; //print out message
		config.storePath = "./store";  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

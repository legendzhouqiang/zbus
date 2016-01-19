package org.zbus.examples;

import org.zbus.kit.ConfigKit;
import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;

public class ZbusStarter {

	/**
	 * Start zbus in embedded mode, simple?
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();  
		config.serverHost = ConfigKit.option(args, "-h", "0.0.0.0");
		config.serverPort = ConfigKit.option(args, "-p", 15555); 
		config.verbose = ConfigKit.option(args, "-verbose", true);
		config.storePath = ConfigKit.option(args, "-store", "store"); 
		@SuppressWarnings("resource")
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

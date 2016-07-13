package org.zbus.examples.ha;

import org.zbus.kit.ConfigKit;
import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;

public class Zbus1 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		String xmlConfigFile = ConfigKit.option(args, "-conf", "conf/ha/zbus1.xml");
		
		MqServerConfig config = new MqServerConfig(); 
		config.loadFromXml(xmlConfigFile);  
		  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

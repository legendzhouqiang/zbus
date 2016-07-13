package org.zbus.examples.ha;

import org.zbus.broker.ha.TrackServer;
import org.zbus.broker.ha.TrackServerConfig;
import org.zbus.kit.ConfigKit;

public class Track1 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		String xmlConfigFile = ConfigKit.option(args, "-conf", "conf/ha/tracker1.xml");
		
		TrackServerConfig config = new TrackServerConfig(); 
		config.loadFromXml(xmlConfigFile); 
		
		TrackServer server = new TrackServer(config);  
		server.start();
	}

}

package org.zbus.examples.ha;

import java.io.InputStream;

import org.zbus.broker.ha.TrackServer;
import org.zbus.broker.ha.TrackServerConfig;

public class Track1 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		InputStream stream = Track1.class.getClassLoader().getResourceAsStream("conf/ha/tracker1.xml");
		
		TrackServerConfig config = new TrackServerConfig(); 
		config.loadFromXml(stream); 
		
		TrackServer server = new TrackServer(config);  
		server.start();
	} 
}

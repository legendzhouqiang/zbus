package io.zbus.examples.ha;

import java.io.InputStream;

import io.zbus.mq.tracker.TrackServer;
import io.zbus.mq.tracker.TrackServerConfig;

public class Track2 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		InputStream stream = Track2.class.getClassLoader().getResourceAsStream("conf/ha/tracker2.xml");
		
		TrackServerConfig config = new TrackServerConfig(); 
		config.loadFromXml(stream); 
		
		TrackServer server = new TrackServer(config);  
		server.start();
	}

}

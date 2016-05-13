package org.zbus.examples.ha;

import org.zbus.ha.TrackServer;

public class Track2 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		TrackServer server = new TrackServer(16667);
		server.setVerbose(true);
		
		server.start();
	}

}

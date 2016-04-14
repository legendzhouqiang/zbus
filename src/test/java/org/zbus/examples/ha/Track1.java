package org.zbus.examples.ha;

import org.zbus.broker.ha.TrackServer;

public class Track1 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		TrackServer server = new TrackServer(16666);
		server.setVerbose(true);
		
		server.start();
	}

}

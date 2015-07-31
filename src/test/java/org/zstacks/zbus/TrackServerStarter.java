package org.zstacks.zbus;

import org.zstacks.zbus.server.TrackServer;

public class TrackServerStarter { 
	
	public static void main(String[] args) throws Exception { 
		args = new String[]{"-p", "16666"};
		TrackServer.main(args);
	}
}

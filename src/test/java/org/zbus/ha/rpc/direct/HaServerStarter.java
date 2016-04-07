package org.zbus.ha.rpc.direct;

import org.zbus.broker.ha.TrackServer;

public class HaServerStarter { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		TrackServer ts = new TrackServer("0.0.0.0:16666");
		ts.start();
	}

}

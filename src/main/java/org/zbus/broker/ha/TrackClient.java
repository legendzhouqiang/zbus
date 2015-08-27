package org.zbus.broker.ha;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.MessageClient;

public class TrackClient extends MessageClient { 
	
	
	public TrackClient(String address, Dispatcher dispatcher) {
		super(address, dispatcher); 
	} 
	public TrackClient(String host, int port, Dispatcher dispatcher) {
		super(host, port, dispatcher); 
	} 
	
	

}

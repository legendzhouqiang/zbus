package org.zbus.broker.ha;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.MessageCodec;

public class TrackServer extends Server {  
	Map<String, List<Entry>> entryTable = new ConcurrentHashMap<String, List<Entry>>();
	
	public TrackServer(Dispatcher dispatcher, IoAdaptor serverAdaptor, int port) {
		super(dispatcher, serverAdaptor, port); 
	} 
	
	public TrackServer(Dispatcher dispatcher, IoAdaptor serverAdaptor, String address) {
		super(dispatcher, serverAdaptor, address); 
	}


	public static void main(String[] args) throws Exception { 
		Dispatcher dispatcher = new Dispatcher();
		IoAdaptor ioAdaptor = new TrackAdaptor();
		
		@SuppressWarnings("resource")
		TrackServer server = new TrackServer(dispatcher, ioAdaptor, 16666);
		server.setServerName("TrackServer");
		server.start();
	}

}

class Entry{
	public String name;
	public String mode; //RPC/MQ/PubSub
	public String serverAddr; 
	public long lastUpdateTime;
	public Object attachment;
}

class TrackAdaptor extends IoAdaptor{
	public TrackAdaptor(){
		codec(new MessageCodec());
	} 
	
	@Override
	protected void onMessage(Object msg, Session sess) throws IOException {
		
	}
}

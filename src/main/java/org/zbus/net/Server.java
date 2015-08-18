package org.zbus.net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

import org.zbus.kit.NetKit;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;

public class Server implements Closeable{
	private static final Logger log = Logger.getLogger(Server.class);  
	
	protected Dispatcher dispatcher; 
	protected String host = "0.0.0.0";
	protected int port;
	
	protected String serverAddr;
	protected String serverName = "Server";
	protected ServerSocketChannel serverChannel;
	
	protected final IoAdaptor serverAdaptor;
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, int port){
		this(dispatcher, serverAdaptor, "0.0.0.0:"+port);
	}
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, String address){
		this.dispatcher = dispatcher;
		this.serverAdaptor = serverAdaptor;
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		this.host = blocks[0];
		this.port = Integer.valueOf(blocks[1]);
		
		if("0.0.0.0".equals(host)){
			serverAddr = String.format("%s:%d",NetKit.getLocalIp(), port);
		}
	}
	
	public void start() throws IOException{  
    	if(serverChannel != null){
    		log.info("Server already started");
    		return;
    	}
    	this.dispatcher.start(); 
    	
    	serverChannel = dispatcher.registerServerChannel(host, port, serverAdaptor);
    	log.info("%s listening [%s:%d]", this.serverName, host, port);
    }
    
    
    @Override
    public void close() throws IOException { 
    	if(serverChannel != null){
    		serverChannel.close();
    		dispatcher.unregisterServerChannel(serverChannel);
    	}
    }
    
    public void setServerName(String serverName){
    	this.serverName = serverName;
    }
}

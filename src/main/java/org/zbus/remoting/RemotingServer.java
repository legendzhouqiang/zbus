package org.zbus.remoting;
 

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.zbus.common.Helper;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
 
public class RemotingServer {  
	private static final Logger log = LoggerFactory.getLogger(RemotingServer.class); 
	protected String serverHost = "0.0.0.0";
	protected int serverPort = 15555;   
	
	protected String serverAddr = String.format("%s:%d",  this.serverHost, this.serverPort);
	protected String serverName = "RemoteServer";
	
	protected ServerEventAdaptor serverHandler;
	protected ServerDispatcherManager dispatcherManager; 
	
	
	public RemotingServer(String serverHost, ServerDispatcherManager dispatcherManager){
		this(serverHost, 15555, dispatcherManager);
	}
	
	public RemotingServer(int serverPort,ServerDispatcherManager dispatcherManager){
		this("0.0.0.0", serverPort,dispatcherManager); 
	}
	
    public RemotingServer(String serverHost, int serverPort, ServerDispatcherManager dispatcherManager) { 
    	this.dispatcherManager = dispatcherManager;
    	this.serverHost = serverHost;
    	this.serverPort = serverPort; 
    	
    	if("0.0.0.0".equals(this.serverHost)){
    		this.serverAddr = String.format("%s:%d", Helper.getLocalIp(), this.serverPort);
    	} else {
    		this.serverAddr = String.format("%s:%d", this.serverHost, this.serverPort);
    	}
    	
    	this.serverHandler = this.dispatcherManager.buildEventAdaptor();
    }   
    
    public void registerGlobalHandler(MessageHandler handler){
    	this.serverHandler.registerGlobalHandler(handler);
    } 
    
    public void registerHandler(String command, MessageHandler handler){
    	this.serverHandler.registerHandler(command, handler);
    }
    
    public void init(){
    	
    }
    
    public void start() throws Exception{   
    	this.init();
    	if(!this.dispatcherManager.isStarted()){
    		dispatcherManager.start();
    	}
    	
    	ServerSocketChannel channel = ServerSocketChannel.open();
    	channel.configureBlocking(false);
    	channel.socket().bind(new InetSocketAddress(this.serverHost, this.serverPort)); 
    	dispatcherManager.getDispatcher(0).registerChannel(channel, SelectionKey.OP_ACCEPT); 
    	log.info("%s serving@%s:%s", this.serverName, this.serverHost, this.serverPort);
    }

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
    
    
}

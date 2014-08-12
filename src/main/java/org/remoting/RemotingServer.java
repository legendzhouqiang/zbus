package org.remoting;
 

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.logging.Logger;
import org.logging.LoggerFactory;
 
public class RemotingServer {  
	private static final Logger log = LoggerFactory.getLogger(RemotingServer.class);
	protected final ServerDispachterManager dispatcherManager;  
	protected boolean ownDispachterManager = false;
	private static volatile ServerDispachterManager defaultDispactherManager = null; 
	
	static ServerDispachterManager getDefaultDispatcherManager(){
		if(defaultDispactherManager == null){
			synchronized (RemotingServer.class) {
				if(defaultDispactherManager == null){
					try {
						defaultDispactherManager = new DefaultServerDispachterManager();
						defaultDispactherManager.start();
					} catch (IOException e) { 
						//ignore
						e.printStackTrace();
					}
				}
			}
		}
		return defaultDispactherManager;
	} 
	
	
	protected String serverHost = "0.0.0.0";
	protected int serverPort = 15555;   
	
	protected String serverAddr = String.format("%s:%d",  this.serverHost, this.serverPort);
	protected String serverName = "RemoteServer";
	
	protected ServerEventAdaptor serverHandler;
	
 
	
	public RemotingServer(String serverHost, int serverPort){
		this(serverHost, serverPort, getDefaultDispatcherManager());
	}
	
	public RemotingServer(int serverPort){
		this("0.0.0.0", serverPort, getDefaultDispatcherManager()); 
	}
	
    public RemotingServer(String serverHost, int serverPort, ServerDispachterManager dispatcherManager) { 
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
    	init();
    	
    	dispatcherManager.start();
    	
    	ServerSocketChannel channel = ServerSocketChannel.open();
    	channel.configureBlocking(false);
    	channel.bind(new InetSocketAddress(this.serverHost, this.serverPort)); 
    	dispatcherManager.getDispatcher(0).registerChannel(channel, SelectionKey.OP_ACCEPT); 
    	log.info("%s serving@%s:%s", this.serverName, this.serverHost, this.serverPort);
    }
    
    public void close(){
    	if(this.ownDispachterManager){
    		this.dispatcherManager.stop();
    	}
    }

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
    
    
}

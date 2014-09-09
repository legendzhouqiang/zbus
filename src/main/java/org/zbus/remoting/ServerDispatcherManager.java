package org.zbus.remoting;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.zbus.remoting.nio.DispatcherManager;
import org.zbus.remoting.nio.Session;

public class ServerDispatcherManager extends DispatcherManager{ 
	protected ServerEventAdaptor serverEventAdaptor = new ServerEventAdaptor();
	
	public ServerDispatcherManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{ 
		super(new MessageCodec(), executor, engineCount, engineNamePrefix);
		
		this.registerHandler(Message.HEARTBEAT, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	
	public ServerDispatcherManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, ServerDispatcherManager.class.getSimpleName());
	}
	

	public ServerDispatcherManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	public ServerEventAdaptor buildEventAdaptor(){ 
		return this.serverEventAdaptor;
	}
	
	public void registerHandler(String command, MessageHandler handler){
    	this.serverEventAdaptor.registerHandler(command, handler);
    }
    
    public void registerGlobalHandler(MessageHandler beforeHandler) {
		this.serverEventAdaptor.registerGlobalHandler(beforeHandler);
	}  
}


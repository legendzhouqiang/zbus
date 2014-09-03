package org.zbus.remoting;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.zbus.remoting.nio.DispatcherManager;

public class ServerDispatcherManager extends DispatcherManager{ 
	protected ServerEventAdaptor serverEventAdaptor = new ServerEventAdaptor();
	
	public ServerDispatcherManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		
		super(new MessageCodec(), executor, engineCount, engineNamePrefix);
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
}


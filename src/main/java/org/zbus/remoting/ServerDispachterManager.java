package org.zbus.remoting;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.zbus.remoting.znet.DispatcherManager;

public abstract class ServerDispachterManager extends DispatcherManager{ 
	public ServerDispachterManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		
		super(new MessageCodec(),  
				executor, 
				engineCount, engineNamePrefix);
	}
	
	public ServerDispachterManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, "ServerDispachterManager");
	}
	

	public ServerDispachterManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	public abstract ServerEventAdaptor buildEventAdaptor();
}


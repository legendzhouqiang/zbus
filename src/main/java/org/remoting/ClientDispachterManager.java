package org.remoting;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.znet.DispatcherManager;

public class ClientDispachterManager extends DispatcherManager{ 
	
	public ClientDispachterManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		
		super(new MessageCodec(),  
				executor, 
				engineCount, engineNamePrefix);
	}
	
	
	public ClientDispachterManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, ClientDispachterManager.class.getName());
	}
	

	public ClientDispachterManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	@Override
	public ClientEventAdaptor buildEventAdaptor() { 
		//每个Session自带独立的处理Handler,不同实例
		return new ClientEventAdaptor();
	}

}


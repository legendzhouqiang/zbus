package org.zbus.remoting;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.zbus.remoting.znet.DispatcherManager;

public class DefaultServerDispachterManager extends ServerDispachterManager{ 
	
	private ServerEventAdaptor serverEventAdaptor = new DefaultServerEventAdaptor();
	
	public DefaultServerDispachterManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		super(executor, engineCount, engineNamePrefix);
	}
	
	
	public DefaultServerDispachterManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, "ServerDispachterManager");
	}
	

	public DefaultServerDispachterManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	@Override
	public ServerEventAdaptor buildEventAdaptor() {  
		//服务器端，所有Session共享一个事件处理器
		return this.serverEventAdaptor;
	}

}


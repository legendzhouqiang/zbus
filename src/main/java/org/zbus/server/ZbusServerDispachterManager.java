package org.zbus.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.remoting.ServerDispachterManager;
import org.remoting.ServerEventAdaptor;
import org.znet.DispatcherManager;

public class ZbusServerDispachterManager extends ServerDispachterManager{ 
	private ServerEventAdaptor serverEventAdaptor = new ZbusServerEventAdaptor();
	
	public ZbusServerDispachterManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		
		super(executor,engineCount, "ZbusServerDispachterManager");
	}
	
	public ZbusServerDispachterManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, "ZbusServerDispachterManager");
	}

	public ZbusServerDispachterManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	@Override
	public ServerEventAdaptor buildEventAdaptor() {  
		//服务器端，所有Session共享一个事件处理器
		return this.serverEventAdaptor;
	}
}


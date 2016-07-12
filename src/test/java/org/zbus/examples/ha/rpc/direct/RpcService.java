package org.zbus.examples.ha.rpc.direct;

import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.direct.Service;
import org.zbus.rpc.direct.ServiceConfig;

public class RpcService {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		//An processor to handle message consumed from remote (zbus/direct client)
		RpcProcessor processor = new RpcProcessor(); 
		processor.addModule(new InterfaceExampleImpl()); 
		
		ServiceConfig config = new ServiceConfig(); 
		
		//Id used by client to address service
		config.entryId = "HaDirectRpc"; 
		
		//Use random port if not set
		//config.serverPort = 8080; 
		
		config.messageProcessor = processor; 
		//TrackServer address list
		config.trackServerList = "127.0.0.1:16666";//"127.0.0.1:16666;127.0.0.1:16667";
		
		Service svc = new Service(config);
		svc.start();
	}  
	
}

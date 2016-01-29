package org.zbus.examples.rpc.direct;

import java.io.IOException;

import org.zbus.examples.rpc.appdomain.InterfaceExampleImpl;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.direct.Service;
import org.zbus.rpc.direct.ServiceConfig;

public class RpcService {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		RpcProcessor processor = new RpcProcessor(); 
		processor.addModule(new InterfaceExampleImpl()); 
		
		ServiceConfig config = new ServiceConfig(); 
		config.serverPort = 8080;  
		config.messageProcessor = processor; 
		
		Service svc = new Service(config);
		svc.start(); 
	} 
}

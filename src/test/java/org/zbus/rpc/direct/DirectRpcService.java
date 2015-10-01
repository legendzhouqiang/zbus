package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class DirectRpcService {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		RpcProcessor processor = new RpcProcessor();
		// 增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl()); 
		
		ServiceConfig config = new ServiceConfig(); 
		config.serverPort = 15555; 
		//config.thriftServer = "0.0.0.0:25555";
		config.messageProcessor = processor; 
		
		Service svc = new Service(config);
		svc.start(); 
	} 
}

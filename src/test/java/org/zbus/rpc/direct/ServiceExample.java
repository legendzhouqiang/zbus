package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.net.core.Dispatcher;
import org.zbus.rpc.RpcProcessor;
import org.zbus.rpc.biz.InterfaceImpl;

public class ServiceExample {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		int port = ConfigKit.option(args, "-port", 8080);
		Dispatcher dispatcher = new Dispatcher();
		
		RpcProcessor processor = new RpcProcessor();
		// 增加模块，模块名在调用时需要指定
		processor.addModule(new InterfaceImpl());
		
		Service svc = new Service(dispatcher, port, processor);
		svc.start(); 
	} 
}

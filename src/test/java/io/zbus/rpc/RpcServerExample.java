package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.RpcBootstrap;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcBootstrap b = new RpcBootstrap();

		b.setStackTraceEnabled(false);
		//b.setAutoLoadService(true);
		//b.setMethodPageModule("m");
		b.addModule("example", InterfaceExampleImpl.class); 
		
		b.setPort(80);
		b.start();
	}
}

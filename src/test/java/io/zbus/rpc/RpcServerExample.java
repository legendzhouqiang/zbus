package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.ServiceBootstrap;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ServiceBootstrap b = new ServiceBootstrap();

		b.setStackTraceEnabled(false);
		//b.setMethodPageModule("m");
		b.addModule("example", InterfaceExampleImpl.class); 
		
		b.setPort(80);
		b.start();
	}
}

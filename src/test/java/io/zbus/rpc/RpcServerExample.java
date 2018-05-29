package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcServerBootstrap b = new RpcServerBootstrap(); 
		
		InterfaceExample example = new InterfaceExampleImpl();
		
		b.setStackTraceEnabled(false);
		//b.setAutoLoadService(true);
		//b.setMethodPageModule("m");
		b.addModule("example", example); 
		
		b.setPort(80);
		b.start();
	}
}

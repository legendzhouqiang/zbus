package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.RpcBootstrap;

 
public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcBootstrap b = new RpcBootstrap(); 
		
		InterfaceExample example = new InterfaceExampleImpl();
		
		b.setStackTraceEnabled(false);
		//b.setAutoLoadService(true);
		//b.setMethodPageModule("m");
		b.addModule("example", example); 
		
		b.setPort(80);
		b.start();
	}
}

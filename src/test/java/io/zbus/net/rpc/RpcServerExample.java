package io.zbus.net.rpc;

import io.zbus.net.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.ServiceBootstrap;

public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		ServiceBootstrap b = new ServiceBootstrap();
		b.addModule("index", InterfaceExampleImpl.class);
		b.port(80);
		b.start();
	}
}

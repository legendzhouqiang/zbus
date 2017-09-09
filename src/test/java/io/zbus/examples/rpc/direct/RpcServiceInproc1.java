package io.zbus.examples.rpc.direct;

import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.examples.rpc.biz.generic.GenericMethodImpl;
import io.zbus.examples.rpc.biz.inheritance.SubService1;
import io.zbus.examples.rpc.biz.inheritance.SubService2;
import io.zbus.rpc.ServiceBootstrap;

public class RpcServiceInproc1 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {    
		ServiceBootstrap b = new ServiceBootstrap();

		b.addModule(InterfaceExampleImpl.class);
		b.addModule(GenericMethodImpl.class);
		b.addModule(SubService1.class);
		b.addModule(SubService2.class); 

		b.serviceName("MyRpc") 
		 .serviceToken("myrpc_service")  //Enable Token authentication
		 .port(15555) //start server inside
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL 
		 
		 .start();
	} 
}

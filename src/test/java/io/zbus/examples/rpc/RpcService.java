package io.zbus.examples.rpc;

import io.zbus.rpc.bootstrap.mq.ServiceBootstrap;

/**
 * Will start zbus server internally
 * 
 * @author Rushmore
 *
 */
public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		ServiceBootstrap b = new ServiceBootstrap(); 
		
		//manually add modules
		//b.addModule(InterfaceExampleImpl.class); 
		//b.addModule(GenericMethodImpl.class);
		//b.addModule(SubService1.class);
		//b.addModule(SubService2.class);  
		
		b.serviceName("MyRpc") // application level entry, full URL: <service> / <module> / <method> 
		 .port(15555)          // start server inside 
		 .autoDiscover(true)   // disable if add modules manually
		 .connectionCount(4)   // parallel connection count
		 //.serviceMask(Protocol.MASK_DISK)
		 .responseTypeInfo(false) //Enable it if Generic method proxy required!!!
		 .verbose(true)
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL
		 //.serviceToken("myrpc_service") //Enable Token authentication
		 .start();
	}
}

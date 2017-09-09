package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.examples.rpc.biz.generic.GenericMethodImpl;
import io.zbus.examples.rpc.biz.inheritance.SubService1;
import io.zbus.examples.rpc.biz.inheritance.SubService2;
import io.zbus.rpc.ServiceBootstrap;

public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		ServiceBootstrap b = new ServiceBootstrap();

		b.addModule(InterfaceExampleImpl.class); 
		b.addModule(GenericMethodImpl.class);
		b.addModule(SubService1.class);
		b.addModule(SubService2.class); 

		
		//Enable SSL + Token based security 
		
		//ServerAddress serverAddress = new ServerAddress("localhost:15555");
		//serverAddress.setCertFile("ssl/zbus.crt");
		//serverAddress.setSslEnabled(true);	 
		//serverAddress.setToken("myrpc_service"); 
		
		b.serviceName("MyRpc")
		 .serviceToken("myrpc_service") 
		 .serviceAddress("localhost:15555;localhost:15556") //connect to remote server
		 .start();
	}
}

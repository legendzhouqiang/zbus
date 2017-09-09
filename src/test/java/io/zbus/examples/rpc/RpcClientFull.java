package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.examples.rpc.biz.generic.GenericMethod;
import io.zbus.examples.rpc.biz.inheritance.SubServiceInterface1;
import io.zbus.examples.rpc.biz.inheritance.SubServiceInterface2;
import io.zbus.rpc.ClientBootstrap;

public class RpcClientFull {

	public static void main(String[] args) throws Exception { 
		//Enable SSL + Token based security 
		
		//ServerAddress serverAddress = new ServerAddress("localhost:15555"); 
		//serverAddress.setToken("myrpc_client");
		//trackerAddress.setCertFile("ssl/zbus.crt");  
		//trackerAddress.setSslEnabled(true); 
		
		ClientBootstrap b = new ClientBootstrap(); 
		b.serviceAddress("localhost:15555;localhost:15556")
		 .serviceName("MyRpc")
		 .serviceToken("myrpc_client"); 
		  
		InterfaceExample api = b.createProxy(InterfaceExample.class); 
		TestCases.testDynamicProxy(api);  //fully test on all cases of parameter types
		
		GenericMethod m = b.createProxy(GenericMethod.class);  
		TestCases.testGenericMethod(m);
		
		SubServiceInterface1 service1 = b.createProxy(SubServiceInterface1.class);
		TestCases.testInheritGeneric1(service1);
		
		SubServiceInterface2 service2 = b.createProxy(SubServiceInterface2.class); 
		TestCases.testInheritGeneric2(service2); 
		
		b.close(); 
	} 
}

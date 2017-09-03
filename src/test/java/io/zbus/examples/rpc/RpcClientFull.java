package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.IBaseExt;
import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.examples.rpc.biz.User;
import io.zbus.mq.Broker;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.transport.ServerAddress;

public class RpcClientFull {

	public static void main(String[] args) throws Exception {  
		ServerAddress trackerAddress = new ServerAddress("localhost:15555"); 
		trackerAddress.setCertFile("ssl/zbus.crt");
		trackerAddress.setSslEnabled(true); 
		trackerAddress.setToken("myrpc_client"); //Token for tracker,  
		
		Broker broker = new Broker();
		broker.addTracker(trackerAddress);
	
		RpcConfig config = new RpcConfig();
		config.setBroker(broker);
		config.setTopic("MyRpc");
		config.setToken("myrpc_client");   //Token for RPC client 
		
		RpcInvoker rpc = new RpcInvoker(config);
		  
		InterfaceExample api = rpc.createProxy(InterfaceExample.class); 
		TestCases.testDynamicProxy(api);  //fully test on all cases of parameter types
		 
		
		IBaseExt baseExt = rpc.createProxy(IBaseExt.class); 
		User user = new User();
		user.setName("rushmore");
		boolean ok = baseExt.save(user);
		System.out.println(ok);
		
		broker.close(); 
	} 
}

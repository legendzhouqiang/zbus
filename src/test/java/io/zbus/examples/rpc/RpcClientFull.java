package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.IBaseExt;
import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.examples.rpc.biz.User;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;

public class RpcClientFull {

	public static void main(String[] args) throws Exception { 
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackerList("localhost:15555");
		brokerConfig.setToken("myrpc_client"); 
		Broker broker = new Broker(brokerConfig);
	
		RpcConfig config = new RpcConfig();
		config.setBroker(broker);
		config.setTopic("MyRpc");
		config.setToken(brokerConfig.getToken()); 
		
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

package io.zbus.examples.rpc;

import io.zbus.mq.Broker;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;

public class RpcInvokerRaw {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555"); 
		RpcConfig config = new RpcConfig(broker);  
		config.setTopic("MyRpc"); 
		
		RpcInvoker rpc = new RpcInvoker(config);
		
		Request req = new Request();
		req.setMethod("plus");
		req.setParams(new Object[]{1,2});
		
		Response res = rpc.invokeSync(req);
		System.out.println(res);
		
		broker.close(); 
	}

}

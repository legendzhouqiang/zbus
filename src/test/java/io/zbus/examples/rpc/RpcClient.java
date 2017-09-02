package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.transport.ResultCallback;

public class RpcClient {

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
		
		//Way 1) Raw request
		Request req = new Request();
		req.setMethod("plus");
		req.setParams(new Object[]{1,2});
		
		Response res = rpc.invokeSync(req);
		System.out.println("raw: " + res.getResult());
		
		//asynchronous call
		rpc.invokeAsync(req, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response result) { 
				Integer res = (Integer)result.getResult(); 
				System.out.println("async raw: " + res);
			}
		});
		
		
		//Way 2) More abbreviated
		int result = rpc.invokeSync(Integer.class, "plus", 1, 2);
		System.out.println("typed: " + result); 
		
		
		
		//Way 3) Dynamic proxy class, the client side only need Interface
		InterfaceExample api = rpc.createProxy(InterfaceExample.class);
		result = api.plus(1, 2); 
		System.out.println("proxy class: " + result);
		
		
		broker.close(); 
	}

}

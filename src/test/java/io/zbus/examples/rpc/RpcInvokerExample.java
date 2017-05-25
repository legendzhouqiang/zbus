package io.zbus.examples.rpc;

import io.zbus.examples.rpc.api.InterfaceExample;
import io.zbus.mq.Broker;
import io.zbus.net.ResultCallback;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcInvoker;

public class RpcInvokerExample {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555");   
	
		RpcInvoker rpc = new RpcInvoker(broker, "MyRpc");
		
		//Way 1) Raw request
		Request req = new Request();
		req.setMethod("plus");
		req.setParams(new Object[]{1,2});
		
		Response res = rpc.invokeSync(req);
		System.out.println(res);
		
		//asynchronous call
		rpc.invokeAsync(req, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response result) { 
				Integer res = (Integer)result.getResult(); 
				System.out.println(res);
			}
		});
		
		
		//Way 2) More abbreviated
		int result = rpc.invokeSync(Integer.class, "plus", 1, 2);
		System.out.println(result); 
		
		
		
		//Way 3) Strong typed proxy
		InterfaceExample api = rpc.createProxy(InterfaceExample.class);
		RpcTestCases.testDynamicProxy(api);  
		
		
		broker.close(); 
	}

}

package io.zbus.examples.rpc;

import io.zbus.mq.Broker;
import io.zbus.net.ResultCallback;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;

public class RpcInvokerAsync {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555"); 
		RpcConfig config = new RpcConfig(broker);  
		config.setTopic("MyRpc"); 
		
		RpcInvoker rpc = new RpcInvoker(config);
		
		Request req = new Request();
		req.setMethod("echo");
		req.setParams(new Object[]{"hong"});
		 
		rpc.invokeAsync(req, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response result) { 
				String res = (String)result.getResult(); 
				System.out.println(res);
			}
		});
		
		Thread.sleep(1000);
		
		broker.close(); 
	}

}

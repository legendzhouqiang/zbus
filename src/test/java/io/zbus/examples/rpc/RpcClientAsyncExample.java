package io.zbus.examples.rpc;

import io.zbus.mq.Broker;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.ZbusBroker;
import io.zbus.net.ResultCallback;
import io.zbus.rpc.RpcInvoker;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.mq.MqInvoker;

public class RpcClientAsyncExample { 
	
	public static void main(String[] args) throws Exception {  
		
		Broker broker = new ZbusBroker(); 
		MessageInvoker mqInvoker = new MqInvoker(broker, "MyRpc");   
		RpcInvoker rpc = new RpcInvoker(mqInvoker); 
		 
		Request request = new Request(); 
		request.setMethod("echo");
		request.setParams(new Object[]{"test"}); 
		
		rpc.invokeAsync(request, new ResultCallback<Response>() { 
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

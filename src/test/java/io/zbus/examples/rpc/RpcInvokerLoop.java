package io.zbus.examples.rpc;

import io.zbus.mq.Broker;
import io.zbus.rpc.RpcInvoker;

public class RpcInvokerLoop {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555;localhost:15556");  
		RpcInvoker rpc = new RpcInvoker(broker, "MyRpc");

		while (true) { 
			try {
				int result = rpc.invokeSync(Integer.class, "plus", 1, 2);
				System.out.println(result); 
			} catch (Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(2000);
		} 
	}

}

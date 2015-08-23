package org.zbus.rpc.direct;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.MessageInvoker;
import org.zbus.pool.Pool;
import org.zbus.rpc.RpcInvoker;

public class RpcRawExample {

	public static void main(String[] args) throws Exception {
		Dispatcher dispatcher = new Dispatcher();
		String serverAddress = "127.0.0.1:8080";
		MessageClientPool clientPool = new MessageClientPool(dispatcher, serverAddress);
		
		Pool<MessageClient> pool = clientPool.getPool();
		
		MessageInvoker invoker = new DirectInvoker(pool); 
		
		RpcInvoker rpc = new RpcInvoker(invoker); 

		String res = rpc.invokeSync(String.class, "getString", "test");
		System.out.println(res);
		
		pool.close();
		dispatcher.close(); 
	}
}

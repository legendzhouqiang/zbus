package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.MessageInvoker;
import org.zbus.pool.Pool;
import org.zbus.rpc.RpcException;

public class DirectInvoker implements MessageInvoker{
	private final Pool<MessageClient> pool;
	
	public DirectInvoker(Pool<MessageClient> pool){
		this.pool = pool;
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		MessageClient client = null;
		try {
			client = pool.borrowObject();
			return client.invokeSync(req, timeout);
		} catch (Exception e) {  
			throw new RpcException(e.getMessage(), e);
		} finally{
			pool.returnObject(client);
		}
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException {  
		MessageClient client = null;
		try {
			client = pool.borrowObject();
			client.invokeAsync(req, callback);
		} catch (Exception e) {  
			throw new RpcException(e.getMessage(), e);
		} finally{
			pool.returnObject(client);
		}
	} 
}

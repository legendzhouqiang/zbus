package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageInvoker;

public class DirectInvoker implements MessageInvoker{
	private final Broker broker;
	
	public DirectInvoker(Broker broker){
		this.broker = broker;
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		return this.broker.invokeSync(req, timeout);
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException {  
		this.broker.invokeAsync(req, callback);
	} 
}

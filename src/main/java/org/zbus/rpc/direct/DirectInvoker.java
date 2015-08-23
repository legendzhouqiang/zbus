package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageInvoker;

public class DirectInvoker implements MessageInvoker {

	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		return null;
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException {  
	} 
}

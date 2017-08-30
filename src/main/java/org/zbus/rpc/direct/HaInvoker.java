package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class HaInvoker implements MessageInvoker{
	private final MessageInvoker messageInvoker;
	private final String entryId;
	
	public HaInvoker(MessageInvoker messageInvoker, String entryId){
		this.messageInvoker = messageInvoker;
		this.entryId = entryId;
	}
	
	private void prepare(Message req){ 
		req.setMq(this.entryId);
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		prepare(req);
		return this.messageInvoker.invokeSync(req, timeout); 
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException { 
		prepare(req);
		this.messageInvoker.invokeAsync(req, callback);
	} 
}

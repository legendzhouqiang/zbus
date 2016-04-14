package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.mq.Protocol;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class MqInvoker implements MessageInvoker{
	private final MessageInvoker messageInvoker;
	private final String mq;
	
	public MqInvoker(MessageInvoker messageInvoker, String mq){
		this.messageInvoker = messageInvoker;
		this.mq = mq;
	}
	
	private void fillBrokerMessage(Message req){
		req.setCmd(Protocol.Produce);
		req.setAck(false); //ACK 必须设置为false，等待service的结果
		req.setMq(this.mq);
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		fillBrokerMessage(req);
		return this.messageInvoker.invokeSync(req, timeout); 
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException { 
		fillBrokerMessage(req);
		this.messageInvoker.invokeAsync(req, callback);
	}

	@Override
	public Message invokeSync(Message req) throws IOException, InterruptedException {
		fillBrokerMessage(req);
		return this.messageInvoker.invokeSync(req);
	} 
}

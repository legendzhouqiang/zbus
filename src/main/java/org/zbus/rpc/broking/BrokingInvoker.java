package org.zbus.rpc.broking;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.mq.Protocol;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageInvoker;

public class BrokingInvoker implements MessageInvoker{
	
	private final Broker broker;
	private final String mq;
	
	public BrokingInvoker(Broker broker, String mq){
		this.broker = broker;
		this.mq = mq;
	}
	
	private void fillBrokerMessage(Message req){
		req.setCmd(Protocol.Produce);
		req.setAck(false);
		req.setMq(this.mq);
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		fillBrokerMessage(req);
		return this.broker.invokeSync(req, timeout); 
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException { 
		fillBrokerMessage(req);
		this.broker.invokeAsync(req, callback);
	} 
}

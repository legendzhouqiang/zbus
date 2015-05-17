package org.zstacks.zbus.client.service;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.MqAdmin;
import org.zstacks.zbus.client.MqConfig;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.znet.Message;
import org.zstacks.znet.ticket.ResultCallback;


public class Caller extends MqAdmin{    
	public Caller(Broker broker, String mq, MessageMode... mode) {
		super(broker, mq, mode);
	}
	
	public Caller(MqConfig config){
		super(config);
	}
	
	private void fillCallMessage(Message req){
		req.setCommand(Proto.Request); 
		req.setMq(this.mq);
		req.setToken(this.accessToken); 
	}
	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		this.fillCallMessage(req); 
		return broker.invokeSync(req, timeout);
	}
	
	public void invokeAsync(Message req, ResultCallback callback) throws IOException{
		this.fillCallMessage(req);   
		broker.invokeAsync(req, callback);
	}
}

package org.zbus.client.service;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.MqAdmin;
import org.zbus.client.MqConfig;
import org.zbus.protocol.MessageMode;
import org.zbus.protocol.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;


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

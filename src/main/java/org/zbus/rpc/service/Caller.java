package org.zbus.rpc.service;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.mq.MqAdmin;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Protocol;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;


public class Caller extends MqAdmin{    
	public Caller(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	}
	
	public Caller(MqConfig config){
		super(config);
	}
	
	private void fillCallMessage(Message req){
		req.setCmd(Protocol.Produce); 
		req.setMq(this.mq); 
		req.setAck(false);
	}

	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		this.fillCallMessage(req); 
		return broker.invokeSync(req, timeout);
	}
	
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException{
		this.fillCallMessage(req);   
		broker.invokeAsync(req, callback);
	}
}

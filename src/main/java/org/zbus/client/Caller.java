package org.zbus.client;

import java.io.IOException;

import org.zbus.common.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;


public class Caller{    
	private final Broker broker; 
	private String mq;
	private String accessToken = "";
	private String registerToken = "";
 
	public Caller(Broker broker, String mq) {
		this.broker = broker;
		this.mq = mq;
	}
	
	public Caller(MqConfig config){
		this.broker = config.getBroker();
		this.mq = config.getMq(); 
		this.accessToken = config.getAccessToken();
		this.registerToken = config.getRegisterToken();
	}
	
	private void fillCallMessage(Message req){
		req.setCommand(Proto.Request); 
		req.setMq(this.mq);
		req.setToken(this.accessToken); 
	}
	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		this.fillCallMessage(req); 
		return broker.produceMessage(req, timeout);
	}
	
	public void invokeAsync(Message req, ResultCallback callback) throws IOException{
		this.fillCallMessage(req);   
		broker.produceMessage(req, callback);
	}

	
	
	public String getMq() {
		return mq;
	}


	public void setMq(String mq) {
		this.mq = mq;
	}


	public String getAccessToken() {
		return accessToken;
	}


	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRegisterToken() {
		return registerToken;
	}

	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}
	
	
}

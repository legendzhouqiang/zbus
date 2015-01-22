package org.zbus.client;

import java.io.IOException;

import org.zbus.common.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;


public class Caller{    
	private final Broker broker; 
	private String mq;
	private String accessToken = "";
 
	public Caller(Broker broker, String mq) {
		this.broker = broker;
		this.mq = mq;
	}
	 
	private void fillCallMessage(Message req){
		req.setCommand(Proto.Request); 
		req.setMq(this.mq);
		req.setToken(this.accessToken); 
	}
	
	private ClientHint myClientHint(){
		ClientHint hint = new ClientHint();
		hint.setMq(mq);
		return hint;
	}
	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		this.fillCallMessage(req); 
		
		RemotingClient client = null;
		try { 
			client = broker.getClient(myClientHint());
			return client.invokeSync(req, timeout);
		} finally {
			if(client != null){
				broker.closeClient(client);
			}
		}
	}
	
	public void invokeAsync(Message req, ResultCallback callback) throws IOException{
		this.fillCallMessage(req);   
		
		RemotingClient client = null;
		try {
			client = broker.getClient(myClientHint());
			client.invokeAsync(req, callback);
		} finally {
			if(client != null){
				broker.closeClient(client);
			}
		}
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
}

package org.zbus.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;

public class Producer { 
	private ClientPool pool;
	private RemotingClient client = null; 
	
	private final String mq;
	private String accessToken = "";  
	private final int mode;
	
	public Producer(ClientPool pool, String mq,  MessageMode... mode){
		this.pool = pool;
		this.mq = mq; 
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		}
	}
	 
	 
	public Producer(RemotingClient client, String mq, MessageMode... mode){
		this.client = client;
		this.mq = mq; 
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		}
	} 
	
	public void send(Message msg, final ResultCallback callback) throws IOException{
		msg.setCommand(Proto.Produce); 
		
		msg.setMq(this.mq);
		msg.setToken(this.accessToken); 
    	if(MessageMode.isEnabled(this.mode, MessageMode.MQ)){
    		InvokeHelper.invokeAsync(this.pool, this.client, msg, callback);
    	} else if(MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
    		InvokeHelper.invokeAsyncAll(this.pool, this.client, msg, callback);
    	} else {
    		throw new IllegalStateException("MessageMode unsupport");
    	}
    }

	public String getAccessToken() {
		return accessToken;
	}


	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}    
	
	public boolean register(String registerToken) throws IOException {
		if(registerToken == null){
			registerToken = "";
		}
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("mq_name", mq);
		params.put("access_token", accessToken);
		params.put("mq_mode", ""+this.mode);
		Message req = Proto.buildAdminMessage(registerToken, Proto.CreateMQ,params);
		Message res = client.invokeSync(req);
		if (res == null) return false;
		return res.isStatus200();
	}
}

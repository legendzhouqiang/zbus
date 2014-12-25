package org.zbus.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;

public class Consumer{   
	private final ClientBuilder clientBuilder;
	private RemotingClient client;      
	private final String mq;            //队列唯一性标识
	private String accessToken = "";    //访问控制码
	private String registerToken = "";  //注册认证码
	private boolean autoRegister = true;  //自动注册
	
	private final boolean useClientBuilder;
	private boolean autoReconnect = true; //自动重连，对Factory模式不起作用
	private final int mode;
	
	private String topic = null;
	
	
	public Consumer(ClientBuilder clientBuilder, String mq, MessageMode... mode){
		this.clientBuilder = clientBuilder;
		this.useClientBuilder = true;
		
		this.mq = mq; 
		this.client = this.clientBuilder.createClientForMQ(this.mq);
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		}
	}
	 
	public Consumer(RemotingClient client, String mq, MessageMode... mode) { 
		this.clientBuilder = null;
		this.useClientBuilder = false;
		
	    this.client = client;
	    this.mq = mq;  
	    if(mode.length == 0){
	    	this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		}
	}  

    public Message recv(int timeout) throws IOException{
    	
    	Message req = new Message();
    	req.setCommand(Proto.Consume);
    	req.setMq(mq);
    	req.setToken(accessToken);  
    	
    	if(MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
    		if(this.topic != null){
    			req.setTopic(this.topic);
    		}
    	}
    	
    	Message res = null;
    	try{ 
    		res = client.invokeSync(req, timeout);
    		if(res != null && res.isStatus404() && this.autoRegister){
    			if(!this.register()){
    				throw new IllegalStateException("register error");
    			}
    			return recv(timeout);
    		}
    	} catch (IOException e){
    		if(useClientBuilder){
    			this.client.close();
    			this.client = this.clientBuilder.createClientForMQ(this.mq) ;
    		} else {
    			if(this.autoReconnect){
    				this.client.ensureConnected();
    			} else {
    				throw e;
    			}
    		}
    	}
    	
    	return res;
    }
    
    
    public void reply(Message msg) throws IOException{
    	msg.setHead(Message.HEADER_REPLY_CODE, msg.getStatus());
    	msg.setCommand(Proto.Produce);  //!!would clear msg's status, do it after getStatus
    	msg.setAck(false);
    	client.getSession().write(msg);
    }
    
    public boolean register() throws IOException{
    	Map<String, String> params = new HashMap<String, String>();
    	params.put("mq_name", mq);
    	params.put("access_token", accessToken);
    	params.put("mq_mode", ""+this.mode);
    	Message req = Proto.buildAdminMessage(registerToken, Proto.CreateMQ, params);
    	Message res = client.invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
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
    
	public boolean isAutoReconnect() {
		return autoReconnect;
	}

	public void setAutoReconnect(boolean autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	public boolean isAutoRegister() {
		return autoRegister;
	}

	public void setAutoRegister(boolean autoRegister) {
		this.autoRegister = autoRegister;
	}
	
	

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		if(!MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
			throw new IllegalStateException("topic support for none-PubSub mode");
		}
		this.topic = topic;
	}

	public void close(){
		if(this.useClientBuilder && this.client != null){
			this.client.close();
		}
	}
}

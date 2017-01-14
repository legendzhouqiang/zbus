package io.zbus.mq;

import java.io.IOException;


public class MqAdmin{     
	protected MessageInvoker messageInvoker;      
	protected String topic;    
	protected Integer flag;
	protected String appid;
	protected String token;  
	protected int invokeTimeout = 10000;// 10s 
	
	public MqAdmin(){
		
	}
	public MqAdmin(MessageInvoker messageInvoker, String topic){  
		this.messageInvoker = messageInvoker;
		this.topic = topic;    
	}  
	 
	protected Message invokeSync(Message req, int timeout) throws IOException, InterruptedException{
		if(messageInvoker == null){
			throw new IllegalStateException("messageInvoker missing");
		}
		return messageInvoker.invokeSync(req, timeout);
	}
	
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		if(messageInvoker == null){
			throw new IllegalStateException("messageInvoker missing");
		}
		messageInvoker.invokeAsync(req, callback);
	}
	
	protected void fillCommonHeaders(Message message){ 
		message.setTopic(this.topic);
		message.setAppid(this.appid);
		message.setToken(this.token); 
	}
	
	public boolean declareTopic() throws IOException, InterruptedException{ 
    	Message req = buildDeclareTopicMessage(); 
    	Message res = invokeSync(req, invokeTimeout);
    	if(res == null) return false;
    	return "200".equals(res.getStatus());
    }    
	
	public Message queryTopic() throws IOException, InterruptedException{
		Message req = new Message();
		fillCommonHeaders(req); 
    	req.setCommand(Protocol.QueryTopic);  
    	
    	return invokeSync(req, invokeTimeout); 
	} 
	
	public boolean removeTopic() throws IOException, InterruptedException{
    	Message req = new Message();
    	fillCommonHeaders(req); 
    	req.setCommand(Protocol.RemoveTopic); 
    	
    	Message res = invokeSync(req, invokeTimeout);
    	if(res == null) return false;
    	return "200".equals(res.getStatus());
    } 
	 
	protected Message buildDeclareTopicMessage(){
		Message req = new Message();
		fillCommonHeaders(req);
    	req.setCommand(Protocol.DeclareTopic);   
    	return req;
	}  
   
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 
 
	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	} 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}   
    
}

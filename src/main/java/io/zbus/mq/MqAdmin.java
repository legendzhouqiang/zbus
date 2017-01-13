package io.zbus.mq;

import java.io.IOException;


public class MqAdmin{     
	protected final Broker broker;      
	protected String topic;    
	protected Integer flag;
	protected String appid;
	protected String token;  
	protected int invokeTimeout = 10000;// 10s
	
	public MqAdmin(Broker broker, String topic){  
		this.broker = broker;
		this.topic = topic;    
	} 
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.topic = config.getTopic();  
		this.appid = config.getAppid();
		this.token = config.getToken();
		this.flag = config.getFlag(); 
	} 
	 
	protected Message invokeSync(Message req) throws IOException, InterruptedException{
		return broker.invokeSync(req, invokeTimeout);
	}
	
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		broker.invokeAsync(req, callback);
	}
	
	protected void fillCommonHeaders(Message message){
		message.setTopic(this.topic);
		message.setAppid(this.appid);
		message.setToken(this.token); 
	}
	
	public boolean declareTopic() throws IOException, InterruptedException{ 
    	Message req = buildDeclareTopicMessage(); 
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return "200".equals(res.getStatus());
    }    
	
	public Message queryTopic() throws IOException, InterruptedException{
		Message req = new Message();
		fillCommonHeaders(req); 
    	req.setCommand(Protocol.QueryTopic);  
    	
    	return invokeSync(req); 
	} 
	
	public boolean removeTopic() throws IOException, InterruptedException{
    	Message req = new Message();
    	fillCommonHeaders(req); 
    	req.setCommand(Protocol.RemoveTopic); 
    	
    	Message res = invokeSync(req);
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

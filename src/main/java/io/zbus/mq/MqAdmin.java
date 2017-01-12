package io.zbus.mq;

import java.io.IOException;

import io.zbus.net.Sync.ResultCallback;


public class MqAdmin{     
	protected final Broker broker;      
	protected String mq;    
	protected Integer flag;
	protected String appid;
	protected String token;     
	
	public MqAdmin(Broker broker, String mq){  
		this.broker = broker;
		this.mq = mq;    
	} 
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.mq = config.getMq();  
		this.appid = config.getAppid();
		this.token = config.getToken();
		this.flag = config.getFlag(); 
	} 
	 
	protected Message invokeSync(Message req) throws IOException, InterruptedException{
		return broker.invokeSync(req, 10000);
	}
	
	protected void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		broker.invokeAsync(req, callback);
	}
	
	protected void fillCommonHeaders(Message message){
		message.setMq(this.mq);
		message.setAppid(this.appid);
		message.setToken(this.token); 
	}
	
	public Message queryQueue() throws IOException, InterruptedException{
		Message req = new Message();
		fillCommonHeaders(req); 
    	req.setCmd(Protocol.QueryMQ);  
    	
    	return invokeSync(req); 
	} 
	protected Message buildDeclareMQMessage(){
		Message req = new Message();
		fillCommonHeaders(req);
    	req.setCmd(Protocol.CreateMQ);   
    	return req;
	}
     
    public boolean declareQueue() throws IOException, InterruptedException{ 
    	Message req = buildDeclareMQMessage(); 
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return "200".equals(res.getStatus());
    }    
    
    public boolean removeQueue() throws IOException, InterruptedException{
    	Message req = new Message();
    	fillCommonHeaders(req); 
    	req.setCmd(Protocol.RemoveMQ); 
    	
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return "200".equals(res.getStatus());
    } 
   
	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
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

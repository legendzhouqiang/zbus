package org.zbus.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;


public class MqAdmin{     
	protected final Broker broker;      
	protected String mq;    
	protected Long flag;
	protected String appid;
	protected String token;    
	protected ConsumeGroup consumeGroup;  
	
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
	
	public Message queryMQ() throws IOException, InterruptedException{
		Message req = new Message();
		fillCommonHeaders(req); 
    	req.setCmd(Protocol.QueryMQ);  
    	
    	return invokeSync(req); 
	}
	
	protected Message buildCreateMQMessage(){
		Message req = new Message();
		fillCommonHeaders(req);
    	req.setCmd(Protocol.CreateMQ);   
    	
    	if(this.consumeGroup != null){
	    	req.setConsumeGroup(consumeGroup.getGroupName());
	    	req.setConsumeBaseGroup(consumeGroup.getBaseGroupName());
	    	req.setConsumeStartOffset(consumeGroup.getStartOffset());
	    	req.setConsumeStartMsgId(consumeGroup.getStartMsgId());
	    	req.setConsumeStartTime(consumeGroup.getStartTime());
		}
    	
    	return req;
	}
    
    public boolean createMQ() throws IOException, InterruptedException{
    	Message req = buildCreateMQMessage(); 
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
    }  
    
    public boolean createMQ(ConsumeGroup consumeGroup) throws IOException, InterruptedException{
    	this.consumeGroup = consumeGroup.clone();
    	
    	Message req = buildCreateMQMessage(); 
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
    }  
    
    public boolean removeMQ() throws IOException, InterruptedException{
    	Message req = new Message();
    	fillCommonHeaders(req); 
    	req.setCmd(Protocol.RemoveMQ); 
    	
    	Message res = invokeSync(req);
    	if(res == null) return false;
    	return res.isStatus200();
    } 
   
	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
	} 
 
	public Long getFlag() {
		return flag;
	}

	public void setFlag(Long flag) {
		this.flag = flag;
	} 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	} 
	
}

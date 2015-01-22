package org.zbus.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;


public class Consumer{  
	private static final Logger log = LoggerFactory.getLogger(Consumer.class);  
	private final Broker broker; 
	private RemotingClient client;      
	private final String mq;            //队列唯一性标识
	private String accessToken = "";    //访问控制码
	private String registerToken = "";  //注册认证码
	private boolean autoRegister = true;  //自动注册
	private final int mode; 
	
	public Consumer(Broker broker, String mq, MessageMode... mode){  
		this.broker = broker;
		this.mq = mq;  
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		}
		this.client = this.broker.getClient(myClientHint());
	}
	 
	private ClientHint myClientHint(){ //NO mq hint
		ClientHint hint = new ClientHint(); 
		return hint;
	}
	 
    public Message recv(int timeout) throws IOException{
    	
    	Message req = new Message();
    	req.setCommand(Proto.Consume);
    	req.setMq(mq);
    	req.setToken(accessToken);  
    	
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
    		try{
    			this.broker.closeClient(this.client);
    			this.client = this.broker.getClient(myClientHint());
    		} catch(Exception ex){
    			log.error(ex.getMessage(), ex);
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
    	params.put("mq_mode", "" + this.mode);
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
	public boolean isAutoRegister() {
		return autoRegister;
	}

	public void setAutoRegister(boolean autoRegister) {
		this.autoRegister = autoRegister;
	} 
	
	public void close(){
		if(this.client != null){
			this.broker.closeClient(this.client);
		}
	}
}

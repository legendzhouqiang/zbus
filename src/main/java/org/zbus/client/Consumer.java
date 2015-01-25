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
	private String fixedBrokerAddress = null; //是否指定Broker,在高可用模式下的选项
	private RemotingClient client;
	private final String mq;            //队列唯一性标识
	private String accessToken = "";    //访问控制码
	private String registerToken = "";  //注册认证码
	private boolean autoRegister = true;  //自动注册
	private final int mode; 
	//为发布订阅者的主题，当Consumer的模式为发布订阅时候起作用
	private String topic = null;
	
	public Consumer(Broker broker, String mq, MessageMode... mode){  
		this.broker = broker;
		this.mq = mq;  
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		} 
	} 
	
	private ClientHint myClientHint(){
		ClientHint hint = new ClientHint();
		hint.setMq(this.mq); 
		hint.setBroker(this.fixedBrokerAddress);
		return hint;
	}
	
	
    public Message recv(int timeout) throws IOException{ 
    	if(this.client == null){
	    	this.client = broker.getConsumerClient(myClientHint());
    	}
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
    	} catch(IOException e){
    		log.error(e.getMessage(), e);
    		try{
    			broker.closeConsumerClient(client);
    			client = broker.getConsumerClient(myClientHint());
    		} catch(IOException ex){
    			log.error(e.getMessage(), e);
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
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		if(!MessageMode.isEnabled(this.mode, MessageMode.PubSub)){
			throw new IllegalStateException("topic support for none-PubSub mode");
		}
		this.topic = topic;
	}

	public String getFixedBrokerAddress() {
		return fixedBrokerAddress;
	}

	public void setFixedBrokerAddress(String fixedBrokerAddress) {
		this.fixedBrokerAddress = fixedBrokerAddress;
	}
	
}

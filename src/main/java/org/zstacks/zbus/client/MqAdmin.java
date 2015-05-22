package org.zstacks.zbus.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.znet.Message;


public class MqAdmin{     
	protected final Broker broker;      
	protected String mq;            //队列唯一性标识
	protected String accessToken = "";    //访问控制码
	protected String registerToken = "";  //注册认证码  
	protected int mode; 
	protected int invokeTimeout = 2500;
	
	public MqAdmin(Broker broker, String mq, MessageMode... mode){  
		this.broker = broker;
		this.mq = mq;  
		if(mode.length == 0){
			this.mode = MessageMode.intValue(MessageMode.MQ); 
		} else {
			this.mode = MessageMode.intValue(mode);
		} 
	} 
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.mq = config.getMq();
		this.accessToken = config.getAccessToken();
		this.registerToken = config.getRegisterToken();
		this.mode = config.getMode();
	}
	
	protected ClientHint myClientHint(){
		ClientHint hint = new ClientHint();
		hint.setMq(this.mq);  
		return hint;
	}
	
	/**
	 * 默认使用broker代理创建，可以覆盖为RemotingClient直接创建，比如Consumer
	 * @param req
	 * @return
	 * @throws IOException
	 */
	protected Message invokeCreateMQ(Message req) throws IOException, InterruptedException{
		return broker.invokeSync(req, invokeTimeout);
	}
   
    public boolean createMQ() throws IOException, InterruptedException{
    	Map<String, String> params = new HashMap<String, String>();
    	params.put("mqName", mq);
    	params.put("accessToken", accessToken);
    	params.put("mqMode", "" + this.mode);
    	
    	Message req = Proto.buildSubCommandMessage(Proto.Admin, Proto.AdminCreateMQ, params);
    	req.setToken(this.registerToken);
    	req.setMq(mq); //支持HA模式下选择Broker算法优化
    	
    	Message res = invokeCreateMQ(req);
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

	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public int getInvokeTimeout() {
		return invokeTimeout;
	}

	public void setInvokeTimeout(int invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
	} 	 
}

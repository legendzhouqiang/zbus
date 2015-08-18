package org.zbus.mq;

import java.io.IOException;

import org.zbus.mq.Broker.ClientHint;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;


public class MqAdmin{     
	protected final Broker broker;      
	protected String mq;            //队列唯一性标识 
	protected final int mode;  
	
	public MqAdmin(Broker broker, String mq, MqMode... mode){  
		this.broker = broker;
		this.mq = mq;  
		if(mode.length == 0){
			this.mode = MqMode.intValue(MqMode.MQ); 
		} else {
			this.mode = MqMode.intValue(mode);
		} 
	} 
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.mq = config.getMq(); 
		this.mode = config.getMode();
	}
	
	protected ClientHint myClientHint(){
		ClientHint hint = new ClientHint();
		hint.setMq(this.mq);  
		return hint;
	}
	
	/**
	 * 默认使用broker代理创建，可以覆盖为Client直接创建，比如Consumer
	 * @param req
	 * @return
	 * @throws IOException
	 */
	protected Message invokeCreateMQ(Message req) throws IOException, InterruptedException{
		return broker.invokeSync(req, 2500);
	}
   
    public boolean createMQ() throws IOException, InterruptedException{
    	Message req = new Message();
    	req.setCmd(Protocol.CreateMQ); 
    	req.setHead("mq_name", mq);
    	req.setHead("mq_mode", "" + mode);
    	
    	Message res = invokeCreateMQ(req);
    	if(res == null) return false;
    	return res.isStatus200();
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
}

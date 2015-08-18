/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

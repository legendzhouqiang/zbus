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

import org.zbus.broker.Broker;
import org.zbus.mq.Protocol.MqMode;

public class MqConfig implements Cloneable { 
	protected Broker broker; //Broker，必须设置
	protected String mq;     //MQ标识，必须设置  
	protected int mode = MqMode.MQ.intValue(); //创建消息队列时采用到
	protected String topic = null; //发布订阅模式下使用
	protected boolean verbose = false;
	protected String registerToken = "";
	protected String accessToken = "";
	protected String masterMq = null;
	protected String masterToken = null;
	
	
	public Broker getBroker() {
		return broker;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
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
	
	public void setMode(MqMode... mode){
		this.mode = MqMode.intValue(mode);
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	
	
	public String getRegisterToken() {
		return registerToken;
	}

	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	} 
	

	public String getMasterMq() {
		return masterMq;
	}

	public void setMasterMq(String masterMq) {
		this.masterMq = masterMq;
	}
 

	public String getMasterToken() {
		return masterToken;
	}

	public void setMasterToken(String masterToken) {
		this.masterToken = masterToken;
	}

	@Override
	public MqConfig clone() { 
		try {
			return (MqConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
}

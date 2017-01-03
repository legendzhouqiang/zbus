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

public class MqConfig implements Cloneable { 
	protected Broker broker;  
	protected String mq;
	protected boolean verbose = false;
	protected String registerToken = "";
	protected String accessToken = ""; 
	
	//control consume group, only consumer controls
	protected String consumeGroup = null;
	protected String consumeBaseGroup = null;
	protected Long consumeStartOffset = null;
	protected String consumeStartMsgId = null;
	protected Long consumeStartTime = null;
	
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
	 
	
	public String getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(String consumeGroup) {
		this.consumeGroup = consumeGroup;
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
	 
	
	public String getConsumeBaseGroup() {
		return consumeBaseGroup;
	}

	public void setConsumeBaseGroup(String consumeBaseGroup) {
		this.consumeBaseGroup = consumeBaseGroup;
	}

	public Long getConsumeStartOffset() {
		return consumeStartOffset;
	}

	public void setConsumeStartOffset(Long consumeStartOffset) {
		this.consumeStartOffset = consumeStartOffset;
	}

	public Long getConsumeStartTime() {
		return consumeStartTime;
	}

	public void setConsumeStartTime(Long consumeStartTime) {
		this.consumeStartTime = consumeStartTime;
	} 

	public String getConsumeStartMsgId() {
		return consumeStartMsgId;
	}

	public void setConsumeStartMsgId(String consumeStartMsgId) {
		this.consumeStartMsgId = consumeStartMsgId;
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

package io.zbus.mq;

import io.zbus.broker.Broker;

public class MqConfig implements Cloneable { 
	protected Broker broker;  
	protected String mq;
	protected String appid;
	protected String token; 
	protected Integer flag;
	protected boolean verbose = false; 
	
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

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	} 
	
	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
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

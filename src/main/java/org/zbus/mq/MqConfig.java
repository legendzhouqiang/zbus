package org.zbus.mq;

import org.zbus.mq.Protocol.MqMode;

public class MqConfig implements Cloneable { 
	protected Broker broker; //Broker，必须设置
	protected String mq;     //MQ标识，必须设置  
	protected int mode = MqMode.MQ.intValue(); //创建消息队列时采用到
	protected String topic = null; //发布订阅模式下使用
	private boolean verbose = false;
	
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
	
	@Override
	public MqConfig clone() { 
		try {
			return (MqConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
}

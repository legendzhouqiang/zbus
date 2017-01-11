package io.zbus.mq;

public class MqConfig implements Cloneable { 
	protected Broker broker;  
	protected String mq;
	protected String appid;
	protected String token; 
	protected Integer flag;
	
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow;
	
	
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
	
	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	} 
	
	public void setConsumeGroup(ConsumeGroup consumeGroup) {
		this.consumeGroup = consumeGroup;
	}  
	
	public void setConsumeGroup(String consumeGroup) {
		this.consumeGroup = new ConsumeGroup(consumeGroup);
	} 

	public Integer getConsumeWindow() {
		return consumeWindow;
	}
	
	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
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

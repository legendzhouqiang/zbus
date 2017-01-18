package io.zbus.mq;

public class MqConfig implements Cloneable { 
	protected Broker broker;  
	protected String topic;
	protected String appid;
	protected String token; 
	protected Integer flag;
	
	protected int invokeTimeout = 10000;  // 10 s
	
	//consume details
	protected ConsumerGroup consumerGroup; 
	protected Integer consumeWindow;
	protected int consumeTimeout = 120000;// 2 minutes
	
	protected boolean verbose = false; 
	
	public Broker getBroker() {
		return broker;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
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
	
	public ConsumerGroup getConsumerGroup() {
		return consumerGroup;
	} 
	
	public void setConsumerGroup(ConsumerGroup consumerGroup) {
		this.consumerGroup = consumerGroup;
	}  
	
	public void setConsumerGroup(String consumeGroup) {
		this.consumerGroup = new ConsumerGroup(consumeGroup);
	} 

	public Integer getConsumeWindow() {
		return consumeWindow;
	}
	
	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	}  
	
	public Integer getConsumeTimeout() {
		return consumeTimeout;
	}

	public void setConsumeTimeout(Integer consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 
	
	public int getInvokeTimeout() {
		return invokeTimeout;
	}

	public void setInvokeTimeout(int invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
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

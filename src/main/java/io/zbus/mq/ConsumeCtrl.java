package io.zbus.mq;

public class ConsumeCtrl implements Cloneable { 
	private String topic;
	private String consumeGroup;  
	
	private Long consumeOffset;   
	private String consumeMsgId; 
	
	private Integer consumeWindow; 

	private int consumeTimeout = 10000;
	
	public ConsumeCtrl(){
		
	}  
	
	public ConsumeCtrl(Message msg){ 
		topic = msg.getTopic();
		consumeGroup = msg.getConsumeGroup();  
		consumeMsgId = msg.getConsumeMsgId();
		consumeOffset = msg.getConsumeOffset(); 
		consumeWindow = msg.getConsumeWindow();
	}
	
	public void writeToMessage(Message msg){
		msg.setTopic(this.getTopic());
		msg.setConsumeGroup(this.consumeGroup);    
		msg.setConsumeMsgId(this.consumeMsgId);
		msg.setConsumeOffset(this.consumeOffset);
		msg.setConsumeWindow(this.consumeWindow);
	} 
	
	
	public void setLocation(Long offset, String msgId) {
		this.consumeOffset = offset;
		this.consumeMsgId = msgId;
	}
	
	public void clearLocation() {
		this.consumeOffset = null;
		this.consumeMsgId = null;
	}
	 
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 

	public String getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(String consumeGroup) {
		this.consumeGroup = consumeGroup;
	}

	public Long getConsumeOffset() {
		return consumeOffset;
	}

	public void setConsumeOffset(Long consumeOffset) {
		this.consumeOffset = consumeOffset;
	}

	public String getConsumeMsgId() {
		return consumeMsgId;
	}

	public void setConsumeMsgId(String consumeMsgId) {
		this.consumeMsgId = consumeMsgId;
	}

	public Integer getConsumeWindow() {
		return consumeWindow;
	}

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	} 

	public int getConsumeTimeout() {
		return consumeTimeout;
	}

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	@Override
	public ConsumeCtrl clone() { 
		try {
			return (ConsumeCtrl)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}

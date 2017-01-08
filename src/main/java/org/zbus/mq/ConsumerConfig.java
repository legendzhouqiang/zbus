package org.zbus.mq;

public class ConsumerConfig extends MqConfig{  
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow;
	
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
	public ConsumerConfig clone() { 
		return (ConsumerConfig)super.clone();
	} 
}

package io.zbus.mq;

public class ConsumerGroup implements Cloneable { 
	private String groupName;
	private String baseGroupName;
	private Long startOffset;
	private String startMsgId;
	private Long startTime;
	private Boolean exclusive;
	private Boolean deleteOnExit;
	private String filterTag;
	
	public ConsumerGroup(){
		
	} 
	
	public ConsumerGroup(String groupName){
		this.groupName = groupName;
	} 
	
	public ConsumerGroup(Message msg){ 
		groupName = msg.getConsumeGroup();
		baseGroupName = msg.getConsumeBaseGroup();
		startOffset = msg.getConsumeStartOffset();
		startTime = msg.getConsumeStartTime();
		startMsgId = msg.getConsumeStartMsgId();
		filterTag = msg.getConsumeFilterTag();
	}
	
	public String getBaseGroupName() {
		return baseGroupName;
	}
	public void setBaseGroupName(String baseGroupName) {
		this.baseGroupName = baseGroupName;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	} 
	public Long getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}
	public String getStartMsgId() {
		return startMsgId;
	}
	public void setStartMsgId(String startMsgId) {
		this.startMsgId = startMsgId;
	}
	public Long getStartTime() {
		return startTime;
	}
	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public Boolean getExclusive() {
		return exclusive;
	}
	public void setExclusive(Boolean exclusive) {
		this.exclusive = exclusive;
	}
	public Boolean getDeleteOnExit() {
		return deleteOnExit;
	}
	public void setDeleteOnExit(Boolean deleteOnExit) {
		this.deleteOnExit = deleteOnExit;
	}   
	
	public String getFilterTag() {
		return filterTag;
	}

	public void setFilterTag(String filterTag) {
		this.filterTag = filterTag;
	}

	@Override
	public ConsumerGroup clone() { 
		try {
			return (ConsumerGroup)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}

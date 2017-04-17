package io.zbus.mq;

public class ConsumeGroup implements Cloneable { 
	private String groupName;
	private String filterTag; //message in group is filtered
	
	//create group from another group
	private String groupCopyFrom;
	
	//create group start from offset, msgId to check valid
	private Long startOffset;
	private String startMsgId;
	
	//create group start from time
	private Long startTime;
	
	//group features
	private Boolean exclusive;
	private Boolean deleteOnExit; 
	
	public ConsumeGroup(){
		
	} 
	
	public ConsumeGroup(String groupName){
		this.groupName = groupName;
	} 
	
	public ConsumeGroup(Message msg){ 
		groupName = msg.getConsumeGroup();
		groupCopyFrom = msg.getConsumeGroupCopyFrom();
		startOffset = msg.getConsumeStartOffset();
		startTime = msg.getConsumeStartTime();
		startMsgId = msg.getConsumeStartMsgId();
		filterTag = msg.getConsumeFilterTag();
	}
	
	public String getGroupCopyFrom() {
		return groupCopyFrom;
	}
	public void setGroupCopyFrom(String groupCopyFrom) {
		this.groupCopyFrom = groupCopyFrom;
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
	public ConsumeGroup clone() { 
		try {
			return (ConsumeGroup)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}

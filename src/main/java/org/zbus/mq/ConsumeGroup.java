package org.zbus.mq;

import org.zbus.net.http.Message;

public class ConsumeGroup implements Cloneable { 
	private String groupName;
	private String baseGroupName;
	private Long startOffset;
	private String startMsgId;
	private Long startTime;
	private Boolean exclusive;
	private Boolean deleteOnExit;
	
	public ConsumeGroup(){
		
	} 
	
	public ConsumeGroup(String groupName){
		this.groupName = groupName;
	} 
	
	public ConsumeGroup(Message msg){ 
		groupName = msg.getConsumeGroup();
		baseGroupName = msg.getConsumeBaseGroup();
		startOffset = msg.getConsumeStartOffset();
		startTime = msg.getConsumeStartTime();
		startMsgId = msg.getConsumeStartMsgId();
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
	
	@Override
	public ConsumeGroup clone() { 
		try {
			return (ConsumeGroup)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "ConsumeGroup [groupName=" + groupName + ", baseGroupName=" + baseGroupName + ", startOffset="
				+ startOffset + ", startMsgId=" + startMsgId + ", startTime=" + startTime + ", exclusive=" + exclusive
				+ ", deleteOnExit=" + deleteOnExit + "]";
	} 
	
}

package io.zbus.mq;

public class ConsumeGroup implements Cloneable { 
	private String groupName;
	private String filterTag; //message in group is filtered
	private Integer groupFlag;
	private String creator;
	
	//create group from another group
	private String startCopy;
	
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
		startCopy = msg.getGroupStartCopy();
		startOffset = msg.getGroupStartOffset();
		startTime = msg.getGroupStartTime();
		startMsgId = msg.getGroupStartMsgId();
		filterTag = msg.getGroupFilter();
		groupFlag = msg.getGroupMask();
		creator = msg.getToken(); //token as creator
	}
	
	public String getStartCopy() {
		return startCopy;
	}
	public void setStartCopy(String groupName) {
		this.startCopy = groupName;
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
	public Integer getGroupFlag() {
		return groupFlag;
	} 
	public void setGroupFlag(Integer groupFlag) {
		this.groupFlag = groupFlag;
	} 
	public String getCreator() {
		return creator;
	} 
	public void setCreator(String creator) {
		this.creator = creator;
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

package io.zbus.mq;

public class ConsumeCtrl { 
	private String groupName;
	private String filterTag;
	private Integer window;
	 
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}  
	public String getFilterTag() {
		return filterTag;
	}

	public void setFilterTag(String filterTag) {
		this.filterTag = filterTag;
	}
	public Integer getWindow() {
		return window;
	}
	public void setWindow(Integer window) {
		this.window = window;
	} 
}

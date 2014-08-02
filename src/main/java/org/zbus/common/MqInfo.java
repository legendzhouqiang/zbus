package org.zbus.common;

import org.json.JSONObject;


public class MqInfo {
	private String broker;
	private String name;
	private long unconsumedMsgCount;
	private long consumerCount;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getUnconsumedMsgCount() {
		return unconsumedMsgCount;
	}
	public void setUnconsumedMsgCount(long unconsumedMsgCount) {
		this.unconsumedMsgCount = unconsumedMsgCount;
	}
	
	public long getConsumerCount() {
		return consumerCount;
	}
	public void setConsumerCount(long consumerCount) {
		this.consumerCount = consumerCount;
	}
	public String getBroker() {
		return broker;
	}
	public void setBroker(String broker) {
		this.broker = broker;
	}
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("broker", this.broker);
		json.put("name", this.name);
		json.put("unconsumedMsgCount", this.unconsumedMsgCount);
		json.put("consumerCount", this.consumerCount); 
		return json.toJSONString();
	} 
	
	public static MqInfo fromJson(JSONObject json){ 
		MqInfo info = new MqInfo();
		info.broker = (String)json.get("broker");
		info.name = (String)json.get("name");
		info.unconsumedMsgCount = (Long)json.get("unconsumedMsgCount");
		info.consumerCount = (Long)json.get("consumerCount"); 
		
		return info;
	}
}

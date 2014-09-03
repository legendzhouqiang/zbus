package org.zbus.common;

import java.util.ArrayList;
import java.util.List;

import org.zbus.common.json.JSONArray;
import org.zbus.common.json.JSONObject;
import org.zbus.common.json.parser.JSONParser;
import org.zbus.remoting.nio.Session.SessionStatus;

public class BrokerMqInfo {
	private String name;
	private long mode;
	private String creator;
	private long createdTime;
	private long unconsumedMsgCount;
	private List<ConsumerInfo> consumerInfoList;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public long getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}
	public long getUnconsumedMsgCount() {
		return unconsumedMsgCount;
	}
	public void setUnconsumedMsgCount(long unconsumedMsgCount) {
		this.unconsumedMsgCount = unconsumedMsgCount;
	}
	public List<ConsumerInfo> getConsumerInfoList() {
		return consumerInfoList;
	}
	public void setConsumerInfoList(List<ConsumerInfo> consumerInfoList) {
		this.consumerInfoList = consumerInfoList;
	}
	
	public MqInfo generateMqInfo(){ 
		MqInfo info = new MqInfo();
		info.setName(this.name);
		info.setUnconsumedMsgCount(unconsumedMsgCount);
		long count = 0;
		if(this.consumerInfoList != null){
			for(ConsumerInfo ci : this.consumerInfoList){
				if(ci.getStatus().equals(SessionStatus.CONNECTED.toString())){
					count++;
				}
			}
		}
		info.setConsumerCount(count);
		
		return info;
	}
	
	public long getMode() {
		return mode;
	}
	public void setMode(long mode) {
		this.mode = mode;
	}
	
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("name", this.name);
		json.put("mode", this.mode);
		json.put("creator", this.creator);
		json.put("createdTime", this.createdTime);
		json.put("unconsumedMsgCount", this.unconsumedMsgCount);
		if(this.consumerInfoList != null){
			JSONArray infoList = new JSONArray();
			for(ConsumerInfo info : this.consumerInfoList){
				infoList.add(info);
			}
			json.put("consumerInfoList", this.consumerInfoList);
		}
		return json.toJSONString();
	} 
	
	public static BrokerMqInfo fromJson(JSONObject json){ 
		BrokerMqInfo info = new BrokerMqInfo();
		info.name = (String)json.get("name");
		info.mode = (Long)json.get("mode");
		info.creator = (String)json.get("creator");
		info.createdTime = (Long)json.get("createdTime");
		if(json.containsKey("consumerInfoList")){
			info.consumerInfoList = new ArrayList<ConsumerInfo>();
			JSONArray consumerInfoList = (JSONArray) json.get("consumerInfoList");
			for(Object consumerInfo : consumerInfoList){
				ConsumerInfo cinfo = ConsumerInfo.fromJson((JSONObject)consumerInfo);
				info.consumerInfoList.add(cinfo);
			}
		}
		return info;
	}
	
	public static void main(String[] args) throws Exception{
		BrokerMqInfo info = new BrokerMqInfo();
		info.name = "xxx";
		info.consumerInfoList = new ArrayList<ConsumerInfo>();
		info.consumerInfoList.add(new ConsumerInfo());
		System.out.println(info);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(info.toString());
		
		BrokerMqInfo info2 = fromJson(json);
		System.out.println(info2);
	}
	 
}

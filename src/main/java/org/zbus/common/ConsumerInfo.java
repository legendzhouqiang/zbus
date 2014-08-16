package org.zbus.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zbus.json.JSONArray;
import org.zbus.json.JSONObject;
import org.zbus.json.parser.JSONParser;

public class ConsumerInfo {
	private String remoteAddr;
	private String status;
	private List<String> topics;
	
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<String> getTopics() {
		return topics;
	}
	public void setTopics(List<String> topics) {
		this.topics = topics;
	}
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("remoteAddr", this.remoteAddr);
		json.put("status", this.status);
		if(this.topics != null){
			JSONArray topics = new JSONArray();
			for(String topic : this.topics){
				topics.add(topic);
			}
			json.put("topics", topics);
		}
		return json.toJSONString();
	} 
	
	public static ConsumerInfo fromJson(JSONObject json){ 
		ConsumerInfo info = new ConsumerInfo();
		info.remoteAddr = (String)json.get("remoteAddr");
		info.status = (String)json.get("status");
		if(json.containsKey("topics")){
			info.topics = new ArrayList<String>();
			JSONArray topics = (JSONArray) json.get("topics");
			for(Object topic : topics){
				info.topics.add((String)topic);
			}
		}
		return info;
	}
	
	
	public static void main(String[] args) throws Exception{
		ConsumerInfo info = new ConsumerInfo();
		info.setRemoteAddr("127.0.0.1:15333");
		info.setStatus("CONNECTED");
		info.setTopics(Arrays.asList("qhee", "xmee"));
		System.out.println(info);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(info.toString());
		ConsumerInfo info2 = fromJson(json);
		System.out.println(info2);
	}
	
	
}

package org.zbus.server.mq.info;

import java.util.HashMap;
import java.util.Map;

import org.zbus.common.json.JSONObject;
import org.zbus.common.json.parser.JSONParser;

public class BrokerInfo{
	long lastUpdatedTime = System.currentTimeMillis();
	String broker;
	Map<String, BrokerMqInfo> mqTable = new HashMap<String, BrokerMqInfo>(); 
	
	public boolean isObsolete(long timeout){
		return (System.currentTimeMillis()-lastUpdatedTime)>timeout;
	}

	public long getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setLastUpdatedTime(long lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	public Map<String, BrokerMqInfo> getMqTable() {
		return mqTable;
	}

	public void setMqTable(Map<String, BrokerMqInfo> mqTable) {
		this.mqTable = mqTable;
	}
  

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		json.put("lastUpdatedTime", this.lastUpdatedTime);
		json.put("broker", this.broker);
		JSONObject mqTable = new JSONObject();
		for(Map.Entry<String, BrokerMqInfo> e : this.mqTable.entrySet()){
			mqTable.put(e.getKey(), e.getValue());
		}
		json.put("mqTable", mqTable);
		return json.toJSONString();
	}    
	
	public static BrokerInfo fromJson(JSONObject json){ 
		BrokerInfo info = new BrokerInfo();
		info.lastUpdatedTime = (Long)json.get("lastUpdatedTime");
		info.broker = (String)json.get("broker");
		
		JSONObject mqTable = (JSONObject)json.get("mqTable");
		for(String key : mqTable.keySet()){
			JSONObject brokerMqInfoJson = (JSONObject)mqTable.get(key);
			info.mqTable.put(key, BrokerMqInfo.fromJson(brokerMqInfoJson));
		}
		return info;
	}
	
	public static void main(String[] args) throws Exception{
		BrokerInfo info = new BrokerInfo();
		info.broker = "127.0.0.1:15555";
		info.lastUpdatedTime = System.currentTimeMillis();
		info.mqTable.put("MyMQ", new BrokerMqInfo());
		
		System.out.println(info);
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(info.toString());
		
		BrokerInfo info2 = BrokerInfo.fromJson(json);
		System.out.println(info2);
	}
}
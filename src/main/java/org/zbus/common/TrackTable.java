package org.zbus.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.parser.JSONParser;

public class TrackTable {
	//broker list
	private List<String> brokerList = new ArrayList<String>();
	private Map<String, List<MqInfo>> mqTable = new HashMap<String, List<MqInfo>>();
	
	public List<String> getBrokerList() {
		return brokerList;
	} 

	public Map<String, List<MqInfo>> getMqTable() {
		return mqTable;
	} 

	public void addBroker(String broker){
		if(brokerList.contains(broker)) return;
		brokerList.add(broker);
	}
	
	public void removeBroker(String broker){
		brokerList.remove(broker);
	}
	
	public String getDefaultBroker(){
		List<String> brokerList = this.brokerList;
		if(brokerList == null || brokerList.size() == 0){
			return null;
		}
		return brokerList.get(0);
	}

	
	public List<MqInfo> getMqInfo(String mq){
		return this.mqTable.get(mq);
	}
	
	public MqInfo nextMqInfo(String mq){
		List<MqInfo> mqInfoList = getMqInfo(mq);
		if(mqInfoList == null || mqInfoList.size()==0) return null;
		MqInfo res = mqInfoList.remove(0);
		mqInfoList.add(res);
		return res;
	}
	
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		JSONArray brokerList = new JSONArray();
		for(String broker : this.brokerList){
			brokerList.add(broker);
		}
		json.put("brokerList", brokerList);
		JSONObject mqTable = new JSONObject();
		for(Entry<String, List<MqInfo>> e: this.mqTable.entrySet()){
			JSONArray mqInfoList = new JSONArray();
			for(MqInfo info : e.getValue()){
				mqInfoList.add(info);
			}
			mqTable.put(e.getKey(), mqInfoList);
		}
		json.put("mqTable", mqTable);
		return json.toJSONString();	
	} 
	
	public static TrackTable fromJson(JSONObject json){ 
		TrackTable info = new TrackTable();
		JSONArray brokerList = (JSONArray) json.get("brokerList");
		for(Object broker : brokerList){
			info.brokerList.add((String)broker);
		}
		JSONObject mqTable = (JSONObject) json.get("mqTable");
		for(String key : mqTable.keySet()){
			JSONArray jsonMqInfoList = (JSONArray) mqTable.get(key);
			List<MqInfo> mqInfoList = new ArrayList<MqInfo>();
			for(Object jsonMqInfo : jsonMqInfoList){
				mqInfoList.add(MqInfo.fromJson((JSONObject)jsonMqInfo));
			}
			info.mqTable.put(key, mqInfoList);
		}
		
		return info;
	}
	
	public static void main(String[] args) throws Exception{
		TrackTable info = new TrackTable();
		info.brokerList = Arrays.asList("127.0.0.1:15555", "127.0.0.1:15556");
		info.mqTable.put("MyMQ", Arrays.asList(new MqInfo()));
		
		System.out.println(info);
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(info.toString());
		
		TrackTable info2 = TrackTable.fromJson(json);
		System.out.println(info2);
	}
}

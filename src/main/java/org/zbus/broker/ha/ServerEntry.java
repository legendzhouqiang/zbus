package org.zbus.broker.ha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


public class ServerEntry implements Comparable<ServerEntry>{
	public static final String RPC    = "RPC";
	public static final String MQ     = "MQ";
	public static final String PubSub = "PubSub";
	
	public String entryId;
	public String serverAddr; 
	public String mode; //RPC/MQ/PubSub 
	
	public long lastUpdateTime; 
	public long unconsumedMsgCount;
	public long consumerCount; 

	public String getEntryId() {
		return entryId;
	}

	public void setEntryId(String id) {
		this.entryId = id;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String setServerAddr) {
		this.serverAddr = setServerAddr;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
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
	
	@Override
	public int compareTo(ServerEntry o) { 
		return (int)(this.consumerCount - o.consumerCount);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serverAddr == null) ? 0 : serverAddr.hashCode());
		result = prime * result + ((entryId == null) ? 0 : entryId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		ServerEntry other = (ServerEntry) obj; 
		if (serverAddr == null) {
			if (other.serverAddr != null) return false;
		} else if (!serverAddr.equals(other.serverAddr)){
			return false;
		}
		
		if (entryId == null) {
			if (other.entryId != null) return false;
		} else if (!entryId.equals(other.entryId)){
			return false;
		}
		return true;
	} 
	
	public static ServerEntry parseJson(String msg){
		return JSON.parseObject(msg, ServerEntry.class);
	}  
	public String toJsonString(){
		return JSON.toJSONString(this);
	}
	
	public static class ServerEntryPrioritySet extends TreeSet<ServerEntry>{ 
		private static final long serialVersionUID = -7110508385050187452L; 
		
		public ServerEntryPrioritySet(){
			
		}
		public ServerEntryPrioritySet(Comparator<ServerEntry> comparator){
			super(comparator);
		} 
		
		public String getMode(){
			ServerEntry be = first();
			if(be == null) return null;
			return be.getMode();
		}
	}
	

	
	public static class HaServerEntrySet{
		//entry_id ==> list of same entries from different target_servers
		Map<String, ServerEntryPrioritySet> entryIdToEntrySet = new ConcurrentHashMap<String, ServerEntryPrioritySet>();
		//server_addr ==> list of entries from same target_server
		Map<String, Set<ServerEntry>> serverToEntrySet = new ConcurrentHashMap<String, Set<ServerEntry>>();
		
		public ServerEntryPrioritySet getPrioritySet(String entryId){
			return entryIdToEntrySet.get(entryId);
		}
		
		public boolean isNewServer(String serverAddr){
			return !serverToEntrySet.containsKey(serverAddr);
		}
		
		public boolean isNewServer(ServerEntry be){
			return isNewServer(be.getServerAddr());
		}
		
		public void updateServerEntry(ServerEntry be){
			String entryId = be.getEntryId(); 
			ServerEntryPrioritySet prioSet = entryIdToEntrySet.get(entryId);
			if(prioSet == null){
				prioSet = new ServerEntryPrioritySet();
				entryIdToEntrySet.put(entryId, prioSet);
			}
			//update
			prioSet.remove(be);
			prioSet.add(be);
			 
			String serverAddr = be.getServerAddr();
			Set<ServerEntry> entries = serverToEntrySet.get(serverAddr);
			if(entries == null){
				entries = Collections.synchronizedSet(new HashSet<ServerEntry>());
				serverToEntrySet.put(serverAddr, entries); 
			}
			entries.remove(be);
			entries.add(be); 
		}
		
		public void removeServer(String serverAddr){
			Set<ServerEntry> serverEntries = serverToEntrySet.get(serverAddr);
			if(serverEntries == null) return;
			for(ServerEntry be : serverEntries){
				Set<ServerEntry> entryOfId = entryIdToEntrySet.get(be.getEntryId());
				if(entryOfId == null) continue; 
				entryOfId.remove(be);
			}
		}
		
		public void removeServerEntry(String serverAddr, String entryId){
			if(serverAddr == null || entryId == null) return;
			
			Set<ServerEntry> serverEntries = serverToEntrySet.get(serverAddr);
			if(serverEntries == null) return;
			Iterator<ServerEntry> iter = serverEntries.iterator();
			while(iter.hasNext()){
				ServerEntry be = iter.next();
				if(entryId.equals(be.getEntryId())){
					iter.remove();
				}
			}
			
			ServerEntryPrioritySet ps = entryIdToEntrySet.get(entryId);
			if(ps == null) return;
			iter = ps.iterator();
			while(iter.hasNext()){
				ServerEntry be = iter.next();
				if(serverAddr.equals(be.getServerAddr())){
					iter.remove();
				}
			} 
		}  
		
		public String toJsonString(){
			return JSON.toJSONString(getAllServerEntries());
		}
		 
		public static List<ServerEntry> parseJson(String jsonString){
			JSONArray jsonArray = JSON.parseArray(jsonString);
			List<ServerEntry> res = new ArrayList<ServerEntry>();
			for(Object obj : jsonArray){
				JSONObject json = (JSONObject)obj;
				ServerEntry entry = JSON.toJavaObject(json, ServerEntry.class);
				res.add(entry);
			}
			return res;
		}
		
		
		public List<ServerEntry> getAllServerEntries(){
			List<ServerEntry> entries = new ArrayList<ServerEntry>();
			for(Set<ServerEntry> set :serverToEntrySet.values()){
				entries.addAll(set);
			}
			return entries;
		}
	}

}


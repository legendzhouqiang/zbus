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


public class BrokerEntry implements Comparable<BrokerEntry>{
	public static final String RPC    = "RPC";
	public static final String MQ     = "MQ";
	public static final String PubSub = "PubSub";
	
	public String id;
	public String mode; //RPC/MQ/PubSub 
	public String broker; 
	
	public long lastUpdateTime; 
	public long unconsumedMsgCount;
	public long consumerCount; 

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
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
	public int compareTo(BrokerEntry o) { 
		return (int)(this.consumerCount - o.consumerCount);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((broker == null) ? 0 : broker.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		BrokerEntry other = (BrokerEntry) obj; 
		if (broker == null) {
			if (other.broker != null) return false;
		} else if (!broker.equals(other.broker)){
			return false;
		}
		
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)){
			return false;
		}
		return true;
	} 
	
	public static BrokerEntry parseJson(String msg){
		return JSON.parseObject(msg, BrokerEntry.class);
	}  
	public String toJsonString(){
		return JSON.toJSONString(this);
	}
	
	public static class BrokerEntryPrioritySet extends TreeSet<BrokerEntry>{ 
		private static final long serialVersionUID = -7110508385050187452L; 
		
		public BrokerEntryPrioritySet(){
			
		}
		public BrokerEntryPrioritySet(Comparator<BrokerEntry> comparator){
			super(comparator);
		} 
		
		public String getMode(){
			BrokerEntry be = first();
			if(be == null) return null;
			return be.getMode();
		}
	}
	

	
	public static class HaBrokerEntrySet{
		//entry_id ==> list of same entries from different brokers
		Map<String, BrokerEntryPrioritySet> entryId2EntrySet = new ConcurrentHashMap<String, BrokerEntryPrioritySet>();
		//broker_addr ==> list of entries from same broker
		Map<String, Set<BrokerEntry>> broker2EntrySet = new ConcurrentHashMap<String, Set<BrokerEntry>>();
		
		public BrokerEntryPrioritySet getPrioritySet(String entryId){
			return entryId2EntrySet.get(entryId);
		}
		
		public boolean isNewBroker(String broker){
			return !broker2EntrySet.containsKey(broker);
		}
		
		public boolean isNewBroker(BrokerEntry be){
			return isNewBroker(be.getBroker());
		}
		
		public void updateBrokerEntry(BrokerEntry be){
			String entryId = be.getId(); 
			BrokerEntryPrioritySet prioSet = entryId2EntrySet.get(entryId);
			if(prioSet == null){
				prioSet = new BrokerEntryPrioritySet();
				entryId2EntrySet.put(entryId, prioSet);
			}
			//update
			prioSet.remove(be);
			prioSet.add(be);
			 
			String brokerAddr = be.getBroker();
			Set<BrokerEntry> entries = broker2EntrySet.get(brokerAddr);
			if(entries == null){
				entries = Collections.synchronizedSet(new HashSet<BrokerEntry>());
				broker2EntrySet.put(brokerAddr, entries); 
			}
			entries.remove(be);
			entries.add(be); 
		}
		
		public void removeBroker(String broker){
			Set<BrokerEntry> brokerEntries = broker2EntrySet.get(broker);
			if(brokerEntries == null) return;
			for(BrokerEntry be : brokerEntries){
				Set<BrokerEntry> entryOfId = entryId2EntrySet.get(be.getId());
				if(entryOfId == null) continue; 
				entryOfId.remove(be);
			}
		}
		
		public void removeBrokerEntry(String broker, String entryId){
			if(broker == null || entryId == null) return;
			
			Set<BrokerEntry> brokerEntries = broker2EntrySet.get(broker);
			if(brokerEntries == null) return;
			Iterator<BrokerEntry> iter = brokerEntries.iterator();
			while(iter.hasNext()){
				BrokerEntry be = iter.next();
				if(entryId.equals(be.getId())){
					iter.remove();
				}
			}
			
			BrokerEntryPrioritySet ps = entryId2EntrySet.get(entryId);
			if(ps == null) return;
			iter = ps.iterator();
			while(iter.hasNext()){
				BrokerEntry be = iter.next();
				if(broker.equals(be.getBroker())){
					iter.remove();
				}
			} 
		}
		
		public String toJsonString(){
			return JSON.toJSONString(entryId2EntrySet);
		}
		
		public List<BrokerEntry> getAllBrokerEntries(){
			List<BrokerEntry> entries = new ArrayList<BrokerEntry>();
			for(Set<BrokerEntry> set :broker2EntrySet.values()){
				entries.addAll(set);
			}
			return entries;
		}
	}

}


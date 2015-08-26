package org.zbus.broker.ha;

import java.util.Comparator;
import java.util.TreeSet;

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
	
	@Override
	public int compareTo(BrokerEntry o) { 
		return 0;
	}

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
	
	
}

class PriorityEntrySet extends TreeSet<BrokerEntry>{ 
	private static final long serialVersionUID = -7110508385050187452L; 
	
	public PriorityEntrySet(){
		
	}
	public PriorityEntrySet(Comparator<BrokerEntry> comparator){
		super(comparator);
	} 
	
	public String getMode(){
		BrokerEntry be = first();
		if(be == null) return null;
		return be.getMode();
	}
}


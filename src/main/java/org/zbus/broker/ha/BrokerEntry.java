package org.zbus.broker.ha;


public class BrokerEntry{
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
	
}


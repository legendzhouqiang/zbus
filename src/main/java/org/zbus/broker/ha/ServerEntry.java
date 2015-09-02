package org.zbus.broker.ha;

import com.alibaba.fastjson.JSON;


public class ServerEntry implements Comparable<ServerEntry>{ 
	public static final int MQ     = 1<<0;
	public static final int PubSub = 1<<1;
	public static final int RPC    = 1<<3;  //参看mq中MqMode
	
	public String entryId;
	public String serverAddr; 
	public int mode; //RPC/MQ/PubSub 
	
	public long lastUpdateTime; 
	public long unconsumedMsgCount;
	public int consumerCount; 
 
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
	
	@Override
	public String toString() {
		return "{server=" + serverAddr
				+ ", mode=" + mode 
				+ ", consumers=" + consumerCount 
				+ ", msgCount=" + unconsumedMsgCount 
				+ ", entry=" + entryId + "}";
	}

	public static ServerEntry parseJson(String msg){
		return JSON.parseObject(msg, ServerEntry.class);
	}  
	
	public String toJsonString(){
		return JSON.toJSONString(this);
	} 
}


package org.zbus.client.agent; 

import org.zbus.client.pool.PoolConfig;


 
public class AgentConfig extends PoolConfig { 
	
	private String trackServerList = "127.0.0.1:16666"; 
	private String seedBroker = "127.0.0.1:15555";
	
	public String getTrackServerList() {
		return trackServerList;
	}

	public void setTrackServerList(String trackServerList) {
		this.trackServerList = trackServerList;
	}

	public String getSeedBroker() {
		return seedBroker;
	}

	public void setSeedBroker(String seedBroker) {
		this.seedBroker = seedBroker;
	}  
}

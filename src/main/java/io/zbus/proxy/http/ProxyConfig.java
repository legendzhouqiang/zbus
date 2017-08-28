package io.zbus.proxy.http;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;

public class ProxyConfig {
	public BrokerConfig brokerConfig;
	public Broker broker;
	public String entry;  //zbus's Topic entry
	public String target; //Target http server 
	public int connectionCount = 4; //Number of connections to zbus broker per consumer 

	public BrokerConfig getBrokerConfig() {
		return brokerConfig;
	}

	public String getEntry() {
		return entry;
	}

	public String getTarget() {
		return target;
	}

	public Broker getBroker() {
		return broker;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public void setBrokerConfig(BrokerConfig brokerConfig) {
		this.brokerConfig = brokerConfig;
	}

	public void setEntry(String entry) {
		this.entry = entry;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
	}

	public void setConnectionCount(int connectionCount) {
		this.connectionCount = connectionCount;
	}
}
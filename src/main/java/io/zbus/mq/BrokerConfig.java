package io.zbus.mq;

import io.zbus.mq.broker.TrackBroker.BrokerSelector;
import io.zbus.net.EventDriver;
 
public class BrokerConfig { 
	public String brokerAddress = "127.0.0.1:15555";   
	public EventDriver eventDriver; 
	public int connectionPoolSize = 32;  
	public BrokerSelector brokerSelector; 
	
	public BrokerConfig() {
		
	}

	public BrokerConfig(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public String getBrokerAddress() {
		return brokerAddress;
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public EventDriver getEventDriver() {
		return eventDriver;
	}

	public void setEventDriver(EventDriver eventDriver) {
		this.eventDriver = eventDriver;
	} 
	public BrokerSelector getBrokerSelector() {
		return brokerSelector;
	}

	public void setBrokerSelector(BrokerSelector brokerSelector) {
		this.brokerSelector = brokerSelector;
	} 

	public int getConnectionPoolSize() {
		return connectionPoolSize;
	}

	public void setConnectionPoolSize(int connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	@Override
	public BrokerConfig clone() {
		try {
			return (BrokerConfig) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}

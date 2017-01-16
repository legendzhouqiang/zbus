package io.zbus.mq;

public class BrokerConfig implements Cloneable { 
	private String brokerAddress = "127.0.0.1:15555";  
	private int connectionPoolSize = 32;   
	
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

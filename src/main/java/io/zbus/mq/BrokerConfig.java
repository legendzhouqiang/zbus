package io.zbus.mq;

/**
 * Broker configuration
 * 1) brokerAddress, ZbusServer(tracker) address
 * 2) connectionPoolSize, physical connection pool size
 * 3) serverInJvm, JvmBroker configuration
 * 
 * @author rushmore (洪磊明) 
 */
public class BrokerConfig implements Cloneable { 
	private String brokerAddress;  
	private int connectionPoolSize = 32;   
	private Object serverInJvm; // Only used for JVM broker, must be instance of io.zbus.mq.server.MqServer
	
	public BrokerConfig(){
		
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
	
	public Object getServerInJvm() {
		return serverInJvm;
	}

	public void setServerInJvm(Object serverInJvm) {
		this.serverInJvm = serverInJvm;
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

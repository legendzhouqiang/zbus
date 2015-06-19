package org.zstacks.zbus.client.service;

import java.util.Arrays;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.MqConfig;

public class ServiceConfig extends MqConfig {
	private ServiceHandler serviceHandler;  
	private int threadCount = 20; 
	private int consumerCount = 1;
	private int readTimeout = 10000;
	private Broker[] brokers; 
	
	public ServiceConfig(){
		super();
	}
	
	public ServiceConfig(Broker... brokers){
		this.brokers = brokers;
		if(brokers.length > 0){
			setBroker(brokers[0]);
		}
	}
	
	public void setBrokers(Broker[] brokers) {
		this.brokers = brokers;
		if(brokers.length > 0){
			setBroker(brokers[0]);
		}
	}
	
	public Broker[] getBrokers(){
		if(brokers == null || brokers.length == 0){
			if(getBroker() != null){
				brokers = new Broker[]{getBroker()};
			}
		}
		return this.brokers;
	}
	
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	} 
	
	public int getConsumerCount() {
		return consumerCount;
	}
	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	}
	public ServiceHandler getServiceHandler() {
		return serviceHandler;
	}
	public void setServiceHandler(ServiceHandler serviceHandler) {
		this.serviceHandler = serviceHandler;
	}
	public int getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	@Override
	public ServiceConfig clone() { 
		return (ServiceConfig)super.clone();
	}
	


	@Override
	public String toString() {
		return "ServiceConfig [serviceHandler=" + serviceHandler
				+ ", threadCount=" + threadCount + ", consumerCount="
				+ consumerCount + ", readTimeout=" + readTimeout + ", brokers="
				+ Arrays.toString(brokers) + ", broker=" + broker + ", mq="
				+ mq + ", accessToken=" + accessToken + ", registerToken="
				+ registerToken + ", mode=" + mode + ", topic=" + topic + "]";
	}

	public static void main(String[] args){
		ServiceConfig config = new ServiceConfig();
		
		ServiceConfig config2 = config.clone();
		config.setMq("hong");
		config2.setThreadCount(2);
		System.err.println(config);
		System.err.println(config2);
	}
	
}

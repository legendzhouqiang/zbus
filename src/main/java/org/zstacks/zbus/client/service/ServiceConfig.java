package org.zstacks.zbus.client.service;

import org.zstacks.zbus.client.MqConfig;




public class ServiceConfig extends MqConfig {
	private ServiceHandler serviceHandler;  
	private int threadCount = 20; 
	private int consumerCount = 1;
	private int readTimeout = 10000;
	
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
}

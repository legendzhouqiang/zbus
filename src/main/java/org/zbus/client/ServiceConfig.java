package org.zbus.client;





public class ServiceConfig {
	private ServiceHandler serviceHandler; 
	private Broker broker;
	private String serviceName;
	
	private String registerToken = "";
	private String accessToken = ""; 
	private int threadCount = 1;
	private int consumeTimeout = 10000;
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	} 
	  
	public Broker getBroker() {
		return broker;
	}
	public void setBroker(Broker broker) {
		this.broker = broker;
	}
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}
	public String getRegisterToken() {
		return registerToken;
	}
	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
	public ServiceHandler getServiceHandler() {
		return serviceHandler;
	}
	public void setServiceHandler(ServiceHandler serviceHandler) {
		this.serviceHandler = serviceHandler;
	}
	public int getConsumeTimeout() {
		return consumeTimeout;
	}
	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 
}

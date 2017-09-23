package io.zbus.proxy.http;

import io.zbus.mq.Broker;

public class ProxyHandlerConfig {
	//Configs to zbus MqServer
	public String topic; 
	public Broker broker; 
	public String token;
	public int consumerCount;
	public int consumeTimeout;
	
	//Configs to target
	public String targetServer; //host:ip
	public String targetUrl;    //after host:ip
	public int targetHeartbeat;  
	public int targetClientCount;
	
	//Send/Recv message filters
	public MessageFilter sendFilter;
	public MessageFilter recvFilter; 
}
package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.transport.ServerAddress;

public class ClientBootstrap implements Closeable{  
	BrokerConfig brokerConfig = null; 
	RpcConfig rpcConfig = new RpcConfig();
	Broker broker; 
	
	public ClientBootstrap serviceAddress(ServerAddress... tracker){
		if(brokerConfig == null){
			brokerConfig = new BrokerConfig();
		}
		for(ServerAddress address : tracker){
			brokerConfig.addTracker(address);
		}
		return this;
	}
	
	public ClientBootstrap serviceAddress(String tracker){
		String[] bb = tracker.split("[;, ]");
		for(String addr : bb){
			addr = addr.trim();
			if("".equals(addr)) continue;
			ServerAddress serverAddress = new ServerAddress(addr);
			serviceAddress(serverAddress);
		}
		return this;
	}
	
	public ClientBootstrap serviceName(String topic){
		rpcConfig.setTopic(topic); 
		return this;
	}
	 
	public ClientBootstrap serviceToken(String token){ 
		rpcConfig.setToken(token);
		return this;
	}  
	
	public RpcInvoker invoker(){
		if(broker == null){
			broker = new Broker(brokerConfig);
		} 
		rpcConfig.setBroker(broker);
		return new RpcInvoker(rpcConfig); 
	}
	
	public <T> T createProxy(Class<T> clazz){  
		return invoker().createProxy(clazz); 
	}   
	
	@Override
	public void close() throws IOException { 
		if(broker != null){
			broker.close();
			broker = null;
		}
	} 
	
}

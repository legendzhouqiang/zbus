package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;
import io.zbus.transport.ServerAddress;

public class ServiceBootstrap implements Closeable{ 
	MqServerConfig serverConfig = null;
	BrokerConfig brokerConfig = null;
	ConsumerConfig consumerConfig = new ConsumerConfig(); 
	
	MqServer mqServer = null;
	Broker broker;
	Consumer consumer;
	RpcProcessor processor = new RpcProcessor();
	
	public ServiceBootstrap port(int port){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setServerPort(port);
		return this;
	} 
	
	public ServiceBootstrap host(String host){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setServerHost(host);
		return this;
	} 
	
	public ServiceBootstrap serviceAddress(ServerAddress... tracker){
		if(brokerConfig == null){
			brokerConfig = new BrokerConfig();
		}
		for(ServerAddress address : tracker){
			brokerConfig.addTracker(address);
		}
		return this;
	}
	
	public ServiceBootstrap serviceAddress(String tracker){
		String[] bb = tracker.split("[;, ]");
		for(String addr : bb){
			addr = addr.trim();
			if("".equals(addr)) continue;
			ServerAddress serverAddress = new ServerAddress(addr);
			serviceAddress(serverAddress);
		}
		return this;
	}
	
	
	public ServiceBootstrap serviceName(String topic){
		consumerConfig.setTopic(topic);
		return this;
	}
	
	public ServiceBootstrap serviceMask(int mask){
		consumerConfig.setTopicMask(mask);
		return this;
	}
	
	public ServiceBootstrap serviceToken(String token){ 
		consumerConfig.setToken(token);
		return this;
	} 
	
	public ServiceBootstrap connectionCount(int connectionCount){ 
		consumerConfig.setConnectionCount(connectionCount);
		return this;
	} 
	
	public ServiceBootstrap start() throws Exception{
		if(serverConfig != null){
			mqServer = new MqServer(serverConfig); 
			mqServer.start();
			broker = new Broker(mqServer);  
		} else {
			broker = new Broker(brokerConfig);
		}
		
		consumerConfig.setBroker(broker);  
		Integer mask = consumerConfig.getTopicMask();
		if(mask == null) {
			mask = Protocol.MASK_MEMORY ;
		}  
		   
		consumerConfig.setTopicMask(mask | Protocol.MASK_RPC); 
		consumerConfig.setMessageHandler(processor);     
		consumer = new Consumer(consumerConfig);
		
		consumer.start();
		return this;
	}  
	
	public ServiceBootstrap addModule(Class<?> clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	
	@Override
	public void close() throws IOException { 
		
	} 
	
}

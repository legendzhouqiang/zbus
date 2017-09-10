package io.zbus.rpc.bootstrap;

import io.zbus.transport.ServerAddress;

public class SpringClientBootstrap extends ClientBootstrap {
	
	public void setServiceAddress(ServerAddress... tracker){
		serviceAddress(tracker);
	}
	
	public void seServiceAddress(String tracker){
		serviceAddress(tracker);
	}
	
	public void setServiceName(String topic){
		serviceName(topic);
	}
	 
	public void setServiceToken(String token){  
		serviceToken(token);
	}  
}

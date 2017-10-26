package io.zbus.rpc.bootstrap;

import io.zbus.transport.ServerAddress;

public class SpringHttpClientBootstrap extends HttpClientBootstrap {
	
	public void setServiceAddress(ServerAddress serverAddress){
		serviceAddress(serverAddress);
	}
	
	public void setServiceAddress(String serverAddress){
		serviceAddress(serverAddress);
	} 
	 
	public void setServiceToken(String token){  
		serviceToken(token);
	}  
	
	public void setClientPoolSize(int clientPoolSize) {
		clientPoolSize(clientPoolSize);
	}
}

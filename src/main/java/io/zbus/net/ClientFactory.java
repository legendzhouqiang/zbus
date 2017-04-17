package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.net.Pool.ObjectFactory;

public abstract class ClientFactory<REQ, RES, T extends Client<REQ, RES>> 
	implements ObjectFactory<T>, Closeable { 
	private static final Logger log = LoggerFactory.getLogger(ClientFactory.class); 
	
	protected final String serverAddress;
	protected EventDriver eventDriver;
	protected boolean ownEventDriver = false;
	
	public ClientFactory(String serverAddress){
		this(serverAddress, new EventDriver());
		this.ownEventDriver = true;
	} 
	
	public ClientFactory(String serverAddress, EventDriver driver){
		this.serverAddress = serverAddress;
		this.eventDriver = driver;
	}
	
	public String getServerAddress(){
		return serverAddress;
	}
	
	@Override
	public boolean validateObject(T client) { 
		if(client == null) return false;
		return client.hasConnected();
	}
	
	@Override
	public void destroyObject(T client){ 
		try {
			client.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e); 
		}
	} 
	 
	public abstract T createObject();
	
	@Override
	public void close() throws IOException {
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	}
}

package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.ClientFactory;
import io.zbus.net.EventDriver;
import io.zbus.net.Pool;

public class MqClientPool implements Closeable {
	private Pool<MqClient> pool; 
	private MqClientFactory factory; 
	 
	private ServerAddress serverAddress;
	private final int clientPoolSize;
	private EventDriver eventDriver;  
	private MqClient detectClient;
	private DisconnectedHandler disconnected;
	
	
	public MqClientPool(String serverAddress, int clientPoolSize, EventDriver eventDriver){   
		this.clientPoolSize = clientPoolSize;
		if(eventDriver != null){
			this.eventDriver = eventDriver.duplicate();
		} else {
			this.eventDriver = new EventDriver();
		}
		this.serverAddress = new ServerAddress(serverAddress, this.eventDriver.isSslEnabled());
		
		this.factory = new MqClientFactory(serverAddress, eventDriver);
		this.pool = new Pool<MqClient>(factory, this.clientPoolSize); 
		
		monitorServer(this.serverAddress);
	}
	
	public MqClientPool(String serverAddress, int clientPoolSize){
		this(serverAddress, clientPoolSize, null);
	}
	
	public MqClientPool(String serverAddress){
		this(serverAddress, 64);
	} 
	
	private void monitorServer(ServerAddress serverAddress){
		detectClient = new MqClient(serverAddress.address, eventDriver);
		try {
			ServerInfo info = detectClient.queryServer();
			this.serverAddress = info.serverAddress;
		} catch (Exception e) {    
			throw new IllegalStateException(serverAddress + " offline");
		}
		
		detectClient.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				if(disconnected != null){
					disconnected.onDisconnected();
				}
			}
		}); 
	} 
	
	public void onDisconnected(DisconnectedHandler disconnected) {
		this.disconnected = disconnected;
	}

	public MqClient borrowClient(){
		try {
			MqClient client = this.pool.borrowObject();  
			return client;
		} catch (Exception e) {
			throw new MqException(e.getMessage(), e);
		} 
	}
	
	public void returnClient(MqClient... client){
		if(client == null || client.length == 0) return;
		for(MqClient c : client){
			this.pool.returnObject(c);
		}
	}
	
	public MqClient createClient(){
		return factory.createObject();
	}
	
	public ServerAddress serverAddress(){
		return serverAddress;
	}  
	
	@Override
	public void close() throws IOException {
		if(this.pool != null){
			this.pool.close(); 
			detectClient.close(); 
			eventDriver.close(); 
			this.pool = null;
		} 
	}
	
	private static class MqClientFactory extends ClientFactory<Message, Message, MqClient>{ 
		public MqClientFactory(String serverAddress) {
			super(serverAddress); 
		}
		
		public MqClientFactory(String serverAddress, EventDriver driver){
			super(serverAddress, driver); 
		} 
		
		public MqClient createObject() { 
			return new MqClient(serverAddress, eventDriver);
		}  
	} 
}

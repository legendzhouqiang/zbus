package org.zbus.rpc.direct;

import java.io.IOException;

import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.MessageClient;
import org.zbus.pool.ObjectFactory;
import org.zbus.pool.Pool;
import org.zbus.pool.PoolConfig;


public class MessageClientPool{
	private static final Logger log = Logger.getLogger(MessageClientPool.class);
	private final Dispatcher dispatcher;
	private final String serverAddress;
	
	public MessageClientPool(Dispatcher dispatcher, String serverAddress){
		this.dispatcher = dispatcher;
		this.serverAddress = serverAddress;
	}
	
	public Pool<MessageClient> getPool(){
		PoolConfig config = new PoolConfig();
		return Pool.getPool(new MessageClientFactory(), config); 
	}
	
	
	private class MessageClientFactory implements ObjectFactory<MessageClient> {		
		@Override
		public boolean validateObject(MessageClient client) { 
			if(client == null) return false;
			return client.hasConnected();
		}
		
		@Override
		public void destroyObject(MessageClient client){ 
			try {
				client.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				//ignore
			}
		}
		
		@Override
		public MessageClient createObject() { 
			return new MessageClient(serverAddress, dispatcher); 
		}
	}
}

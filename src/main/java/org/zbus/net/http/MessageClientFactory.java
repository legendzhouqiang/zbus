package org.zbus.net.http;

import java.io.Closeable;
import java.io.IOException;

import org.zbus.kit.log.Logger;
import org.zbus.kit.pool.ObjectFactory;
import org.zbus.net.core.SelectorGroup;

/**
 * This factory is mainly used by Pool in kit package, a dynamic MessageClient pool can be 
 * created by Pool.getPool(MessageClientFactory instance, poolConfig)
 * 
 * The underlying pool implementation is decided by Pool class.
 * @author rushmore (洪磊明)
 *
 */
public class MessageClientFactory  implements ObjectFactory<MessageClient>, Closeable {
	private static final Logger log = Logger.getLogger(MessageClientFactory.class); 
	private String serverAddress;
	private SelectorGroup selectorGroup;  
	private boolean ownSelectorGroup = false;
	
	public MessageClientFactory(String serverAddress){
		this.serverAddress = serverAddress;
		this.selectorGroup = new SelectorGroup();
		this.ownSelectorGroup = true;
	}
	
	public MessageClientFactory(String serverAddress, SelectorGroup selectorGroup){
		this.serverAddress = serverAddress;
		this.selectorGroup = selectorGroup;
	}
	
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
		}
	}
	
	@Override
	public MessageClient createObject() { 
		return new MessageClient(serverAddress, selectorGroup); 
	}

	@Override
	public void close() throws IOException {
		if(this.ownSelectorGroup){
			if(this.selectorGroup != null){
				this.selectorGroup.close();
				this.selectorGroup = null;
			}
		}
		
	}
}

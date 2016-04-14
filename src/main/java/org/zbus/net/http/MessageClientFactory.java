package org.zbus.net.http;

import org.zbus.net.ClientFactory;
import org.zbus.net.EventDriver;

/**
 * This factory is mainly used by Pool in kit package, a dynamic MessageClient pool can be 
 * created by Pool.getPool(MessageClientFactory instance, poolConfig)
 * 
 * The underlying pool implementation is decided by Pool class.
 * @author rushmore (洪磊明)
 *
 */
public class MessageClientFactory extends ClientFactory<Message, Message, MessageClient> { 
	public MessageClientFactory(String serverAddress){
		super(serverAddress);
	}
	
	public MessageClientFactory(String serverAddress, boolean enableDefaultNio){
		super(serverAddress, enableDefaultNio);
	}
	
	public MessageClientFactory(String serverAddress, EventDriver driver){
		super(serverAddress, driver); 
	} 

	public MessageClient createObject() { 
		return new MessageClient(serverAddress, eventDriver);
	}  
}

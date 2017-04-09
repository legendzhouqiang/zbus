package io.zbus.mq.net;

import io.zbus.mq.Message;
import io.zbus.net.ClientFactory;
import io.zbus.net.EventDriver;

/** 
 * 
 * @author rushmore (洪磊明)
 *
 */
public class MessageClientFactory extends ClientFactory<Message, Message, MessageClient> { 
	public MessageClientFactory(String serverAddress){
		super(serverAddress);
	}
	
	public MessageClientFactory(String serverAddress, EventDriver driver){
		super(serverAddress, driver); 
	} 

	public MessageClient createObject() { 
		return new MessageClient(serverAddress, eventDriver);
	}  
}

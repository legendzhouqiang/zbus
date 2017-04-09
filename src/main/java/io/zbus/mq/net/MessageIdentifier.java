package io.zbus.mq.net;

import io.zbus.mq.Message;
import io.zbus.net.Identifier;

public class MessageIdentifier implements Identifier<Message>{
	@Override
	public void setId(Message msg, String id) { 
		msg.setId(id);
	} 
	@Override
	public String getId(Message msg) {
		return msg.getId();
	}
	
	public final static MessageIdentifier INSTANCE = new MessageIdentifier();
}

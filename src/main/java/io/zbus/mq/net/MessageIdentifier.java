package io.zbus.mq.net;

import io.zbus.mq.Message;
import io.zbus.net.Identifier;

public class MessageIdentifier implements Identifier<Message>{
	@Override
	public void setId(Message request, String id) { 
		request.setId(id);
	} 
	@Override
	public String getId(Message response) {
		return response.getId();
	}
	
	public final static MessageIdentifier INSTANCE = new MessageIdentifier();
}

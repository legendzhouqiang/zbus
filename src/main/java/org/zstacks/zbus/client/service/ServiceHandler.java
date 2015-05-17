package org.zstacks.zbus.client.service; 

import org.zstacks.znet.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

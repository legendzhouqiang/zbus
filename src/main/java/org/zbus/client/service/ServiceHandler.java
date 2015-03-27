package org.zbus.client.service; 

import org.zbus.remoting.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

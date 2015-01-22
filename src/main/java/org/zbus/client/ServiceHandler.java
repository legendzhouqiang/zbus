package org.zbus.client; 

import org.zbus.remoting.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

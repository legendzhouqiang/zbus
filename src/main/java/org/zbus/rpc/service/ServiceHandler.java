package org.zbus.rpc.service; 

import org.zbus.net.http.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

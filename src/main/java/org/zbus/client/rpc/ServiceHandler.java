package org.zbus.client.rpc; 

import org.zbus.remoting.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

package org.zbus.client.rpc; 

import org.remoting.Message;

public interface ServiceHandler { 
	public Message handleRequest(Message request);
}

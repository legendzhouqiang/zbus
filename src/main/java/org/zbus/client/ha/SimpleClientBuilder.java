package org.zbus.client.ha;

import org.zbus.client.ClientBuilder;
import org.zbus.remoting.ClientDispachterManager;
import org.zbus.remoting.RemotingClient;
 
 
public class SimpleClientBuilder implements ClientBuilder{  
	private final ClientDispachterManager clientMgr;
	private final String defaultBroker;
	public SimpleClientBuilder(String defaultBroker){ 
		this(defaultBroker, null);
	}
	
	public SimpleClientBuilder(String defaultBroker, ClientDispachterManager clientMgr){ 
		this.defaultBroker = defaultBroker;
		this.clientMgr = clientMgr;
	}
	 
	public RemotingClient createClientForBroker(String broker){
		return new RemotingClient(broker, this.clientMgr);
	}
	
	public RemotingClient createClientForMQ(String mq){
		return new RemotingClient(this.defaultBroker, this.clientMgr);
	}  
}



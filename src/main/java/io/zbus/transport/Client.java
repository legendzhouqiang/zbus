package io.zbus.transport;

import io.zbus.transport.http.WebsocketClient;
import io.zbus.transport.inproc.InprocClient;

public class Client extends CompositeClient {

	public Client(String address) { 
		support = new WebsocketClient(address);
	}
	
	public Client(IoAdaptor ioAdaptor) {
		support = new InprocClient(ioAdaptor);
	}
}

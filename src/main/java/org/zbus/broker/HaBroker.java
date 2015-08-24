package org.zbus.broker;

import java.io.IOException;

import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class HaBroker implements Broker { 
	
	public HaBroker(BrokerConfig config) throws IOException{ 
		
	}
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException,
			InterruptedException { 
		return null;
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback)
			throws IOException { 

	}

	@Override
	public void close() throws IOException { 

	}

	@Override
	public MessageClient getClient(ClientHint hint) throws IOException { 
		return null;
	}

	@Override
	public void closeClient(MessageClient client) throws IOException { 

	}

}

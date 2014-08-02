package org.zbus.client;

import org.remoting.RemotingClient;

public interface ClientBuilder{
	RemotingClient createClientForMQ(String mq);
	RemotingClient createClientForBroker(String broker); 
}
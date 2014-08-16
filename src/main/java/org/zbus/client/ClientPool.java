package org.zbus.client;

import java.util.List;

import org.zbus.remoting.RemotingClient;

public interface ClientPool{
	RemotingClient borrowClient(String mq) throws Exception;
	List<RemotingClient> borrowEachClient(String mq) throws Exception;
	void returnClient(RemotingClient client) throws Exception;
	void returnClient(List<RemotingClient> clients) throws Exception;
	void invalidateClient(RemotingClient client) throws Exception;
	void destroy();
}
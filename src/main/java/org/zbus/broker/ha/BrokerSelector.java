package org.zbus.broker.ha;

import java.io.Closeable;
import java.util.List;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public interface BrokerSelector extends Closeable{
	Broker selectByBrokerHint(BrokerHint hint);
	List<Broker> selectByRequestMsg(Message msg);
	String getEntry(Message msg);
	Broker selectByClient(MessageClient client);
}
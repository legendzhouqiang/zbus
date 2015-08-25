package org.zbus.broker.ha;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.broker.Broker.ClientHint;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MqBrokerSelector implements BrokerSelector{
	Map<String, List<Entry>> entryTable = new ConcurrentHashMap<String, List<Entry>>();
	Map<String, SingleBroker> brokerTable = new ConcurrentHashMap<String, SingleBroker>();

	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		if(hint.getBroker() != null){
			Broker broker = brokerTable.get(hint.getBroker());
			if(broker != null) return broker;
		}
		
		
		return null;
	}

	@Override
	public List<Broker> selectByRequestMsg(Message msg) { 
		return null;
	}

	@Override
	public Broker selectByClient(MessageClient client) { 
		return null;
	}
	
}
package org.zbus.broker.ha;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.ClientHint;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MqBrokerSelector implements BrokerSelector{
	Map<String, PriorityEntrySet> entryTable = new ConcurrentHashMap<String, PriorityEntrySet>();
	Map<String, SingleBroker> brokerTable = new ConcurrentHashMap<String, SingleBroker>();

	private Broker getBroker(String brokerAddr){
		if(brokerAddr == null) return null;
		return brokerTable.get(brokerAddr); 
	}
	
	private Broker getBroker(BrokerEntry entry){
		if(entry == null) return null; 
		return brokerTable.get(entry.getBroker()); 
	}
	
	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		Broker broker = getBroker(hint.getBroker());
		if(broker != null) return broker;
		
		if(hint.getEntry() != null){
			PriorityEntrySet p = entryTable.get(hint.getEntry());
			if(p != null){
				BrokerEntry e = p.first(); 
				broker = getBroker(e.getBroker());
				if(broker != null) return broker;
			}
		} 
		
		return null;
	}

	@Override
	public List<Broker> selectByRequestMsg(Message msg) {  
		Broker broker = getBroker(msg.getBroker());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = msg.getMq();
		
		PriorityEntrySet p = entryTable.get(entry);
		if(p == null || p.size()==0) return null;
		

		String mode = p.getMode();
		if(BrokerEntry.PubSub.equals(mode)){
			List<Broker> res = new ArrayList<Broker>();
			for(BrokerEntry e : p){ 
				broker = getBroker(e);
				if(broker != null){
					res.add(broker);
				}
			}
		} else {
			broker = getBroker(entry);
			if(broker != null){
				return Arrays.asList(broker);
			}
		}
		
		return null;
	}

	@Override
	public Broker selectByClient(MessageClient client) { 
		String brokerAddr = client.attr("broker");
		if(brokerAddr == null) return null;
		return getBroker(brokerAddr); 
	}
}
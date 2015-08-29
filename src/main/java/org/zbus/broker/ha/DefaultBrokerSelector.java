package org.zbus.broker.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.ClientHint;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.broker.ha.BrokerEntry.BrokerEntryPrioritySet;
import org.zbus.broker.ha.BrokerEntry.HaBrokerEntrySet;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class DefaultBrokerSelector implements BrokerSelector{
	private static final Logger log = Logger.getLogger(DefaultBrokerSelector.class);
	
	private HaBrokerEntrySet haBrokerEntrySet = new HaBrokerEntrySet();
	private Map<String, Broker> allBrokers = new ConcurrentHashMap<String, Broker>();
	
	private TrackSub trackSub;
	
	private Dispatcher dispatcher = null;
	private boolean ownDispatcher = false;
	private BrokerConfig config;
	
	public DefaultBrokerSelector(BrokerConfig config){
		this.config = config;
		if(config.getDispatcher() == null){
			this.ownDispatcher = true;
			this.dispatcher = new Dispatcher();
			this.config.setDispatcher(dispatcher);
		} else {
			this.dispatcher = config.getDispatcher();
			this.ownDispatcher = false;
		}
		this.dispatcher.start(); 
		
		trackSub = new TrackSub(config.getTrackServerList(), dispatcher);
		
		trackSub.onMessage(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				log.info("%s", msg);
				BrokerEntry be = null;
				try{
					be = BrokerEntry.parseJson(msg.getBodyString());
				} catch(Exception e){
					log.error(e.getMessage(), e);
					return;
				}
				boolean isNewBroker = haBrokerEntrySet.isNewBroker(be);
				
				haBrokerEntrySet.updateBrokerEntry(be);
				
				if(isNewBroker){
					onNewBroker(be.getBroker());
				}
			}
		});
		
		trackSub.start();
	}
	
	private void onNewBroker(final String brokerAddr) throws IOException{
		synchronized (allBrokers) {
			if(allBrokers.containsKey(brokerAddr)) return;
			Broker broker = new SingleBroker(this.config);
			allBrokers.put(brokerAddr, broker); 
		} 
	}
	
	private Broker getBroker(String brokerAddr){
		if(brokerAddr == null) return null;
		return allBrokers.get(brokerAddr); 
	}
	
	private Broker getBroker(BrokerEntry entry){
		if(entry == null) return null; 
		return allBrokers.get(entry.getBroker()); 
	}
	
	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		Broker broker = getBroker(hint.getBroker());
		if(broker != null) return broker;
		
		if(hint.getEntry() != null){
			BrokerEntryPrioritySet p = haBrokerEntrySet.getPrioritySet(hint.getEntry());
			if(p != null){
				BrokerEntry e = p.first(); 
				broker = getBroker(e.getBroker());
				if(broker != null) return broker;
			}
		} 
		
		return null;
	}

	@Override
	public String getEntry(Message msg) {
		return msg.getMq();
	}
	
	@Override
	public List<Broker> selectByRequestMsg(Message msg) {  
		Broker broker = getBroker(msg.getBroker());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		
		BrokerEntryPrioritySet p = haBrokerEntrySet.getPrioritySet(entry);
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
	
	@Override
	public void close() throws IOException { 
		if(allBrokers != null){
			for(Broker broker : allBrokers.values()){
				broker.close();
			}
			allBrokers.clear();
		}
		
		if(ownDispatcher){
			dispatcher.close();
		}
	}
}


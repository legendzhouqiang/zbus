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
import org.zbus.broker.ha.ServerEntry.ServerEntryPrioritySet;
import org.zbus.broker.ha.ServerEntry.HaServerEntrySet;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class DefaultBrokerSelector implements BrokerSelector{
	private static final Logger log = Logger.getLogger(DefaultBrokerSelector.class);
	
	private HaServerEntrySet haBrokerEntrySet = new HaServerEntrySet();
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
		
		trackSub.cmd(HaCommand.TargetJoin, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String broker = msg.getServer();
				if(haBrokerEntrySet.isNewServer(broker)){
					onNewServer(broker);
				}
			}
		});
		
		trackSub.cmd(HaCommand.TargetLeave, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String brokerAddr = msg.getServer();
				haBrokerEntrySet.removeServer(brokerAddr);
				synchronized (allBrokers) {
					Broker broker = allBrokers.remove(brokerAddr);
					if(broker == null) return;
					broker.close(); 
				}
			}
		}); 
		
		trackSub.cmd(HaCommand.EntryUpdate, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {  
				ServerEntry be = null;
				try{
					be = ServerEntry.parseJson(msg.getBodyString());
				} catch(Exception e){
					log.error(e.getMessage(), e);
					return;
				}
				boolean isNewBroker = haBrokerEntrySet.isNewServer(be);
				
				haBrokerEntrySet.updateServerEntry(be);
				
				if(isNewBroker){
					onNewServer(be.getServerAddr());
				}
			}
		});
		
		trackSub.cmd(HaCommand.EntryRemove, new MessageHandler() {
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String broker = msg.getServer();
				String entryId = msg.getMq();
				haBrokerEntrySet.removeServerEntry(broker, entryId);
			} 
		});
		
		trackSub.start();
	}
	
	private void onNewServer(final String serverAddr) throws IOException{
		synchronized (allBrokers) {
			if(allBrokers.containsKey(serverAddr)) return;
			this.config.setServerAddress(serverAddr);
			Broker broker = new SingleBroker(this.config);
			allBrokers.put(serverAddr, broker); 
		} 
	}
	
	private Broker getBroker(String serverAddr){
		if(serverAddr == null) return null;
		return allBrokers.get(serverAddr); 
	}
	
	private Broker getBroker(ServerEntry entry){
		if(entry == null) return null; 
		return allBrokers.get(entry.getServerAddr()); 
	}
	
	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		Broker broker = getBroker(hint.getServer());
		if(broker != null) return broker;
		
		if(hint.getEntry() != null){
			ServerEntryPrioritySet p = haBrokerEntrySet.getPrioritySet(hint.getEntry());
			if(p != null){
				ServerEntry e = p.first(); 
				broker = getBroker(e.getServerAddr());
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
		Broker broker = getBroker(msg.getServer());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		
		ServerEntryPrioritySet p = haBrokerEntrySet.getPrioritySet(entry);
		if(p == null || p.size()==0) return null;
		

		String mode = p.getMode();
		if(ServerEntry.PubSub.equals(mode)){
			List<Broker> res = new ArrayList<Broker>();
			for(ServerEntry e : p){ 
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


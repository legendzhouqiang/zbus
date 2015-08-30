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
	
	private HaServerEntrySet haServerEntrySet = new HaServerEntrySet();
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
		
		trackSub.cmd(HaCommand.ServerJoin, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				if(haServerEntrySet.isNewServer(serverAddr)){
					onNewServer(serverAddr);
				}
			}
		});
		
		trackSub.cmd(HaCommand.ServerLeave, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				haServerEntrySet.removeServer(serverAddr);
				synchronized (allBrokers) {
					Broker broker = allBrokers.remove(serverAddr);
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
				boolean isNewServer = haServerEntrySet.isNewServer(be);
				
				haServerEntrySet.updateServerEntry(be);
				
				if(isNewServer){
					onNewServer(be.getServerAddr());
				}
			}
		});
		
		trackSub.cmd(HaCommand.EntryRemove, new MessageHandler() {
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				String entryId = msg.getMq();
				haServerEntrySet.removeServerEntry(serverAddr, entryId);
			} 
		});
		
		trackSub.start();
	}
	
	private void onNewServer(final String serverAddr) throws IOException{
		synchronized (allBrokers) {
			if(allBrokers.containsKey(serverAddr)) return;
			BrokerConfig brokerConfig = this.config.clone();
			brokerConfig.setServerAddress(serverAddr);
			Broker broker = new SingleBroker(brokerConfig);
			allBrokers.put(serverAddr, broker); 
		} 
	}

	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		Broker broker = allBrokers.get(hint.getServer());
		if(broker != null) return broker;
		
		if(hint.getEntry() != null){
			ServerEntryPrioritySet p = haServerEntrySet.getPrioritySet(hint.getEntry());
			if(p != null){
				ServerEntry e = p.first(); 
				broker = allBrokers.get(e.getServerAddr());
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
		Broker broker = allBrokers.get(msg.getServer());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		
		ServerEntryPrioritySet p = haServerEntrySet.getPrioritySet(entry);
		if(p == null || p.size()==0) return null;
		

		String mode = p.getMode();
		if(ServerEntry.PubSub.equals(mode)){
			List<Broker> res = new ArrayList<Broker>();
			for(ServerEntry e : p){ 
				broker = allBrokers.get(e);
				if(broker != null){
					res.add(broker);
				}
			}
		} else {
			broker = allBrokers.get(entry);
			if(broker != null){
				return Arrays.asList(broker);
			}
		}
		
		return null;
	}

	@Override
	public Broker selectByClient(MessageClient client) { 
		String brokerAddr = client.attr("server"); 
		return allBrokers.get(brokerAddr); 
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


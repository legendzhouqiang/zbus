package org.zbus.broker.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.ClientHint;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.broker.ha.ServerEntryTable.ServerList;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class DefaultBrokerSelector implements BrokerSelector{
	private static final Logger log = Logger.getLogger(DefaultBrokerSelector.class);
	
	private final ServerEntryTable haServerEntrySet = new ServerEntryTable();
	private Map<String, Broker> allBrokers = new ConcurrentHashMap<String, Broker>();
	
	private TrackSub trackSub;
	
	private Dispatcher dispatcher = null;
	private boolean ownDispatcher = false;
	private BrokerConfig config;
	
	private CountDownLatch syncFromTracker = new CountDownLatch(1);
	private int syncFromTrackerTimeout = 3000;
	
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
		
		subscribeNotification(); 
	}
	
	private void subscribeNotification(){
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
					log.info("Destroy broker to(%s)", serverAddr);
					broker.close(); 
				}
			}
		}); 
		
		trackSub.cmd(HaCommand.EntryUpdate, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {  
				ServerEntry entry = null;
				try{
					entry = ServerEntry.parseJson(msg.getBodyString());
				} catch(Exception e){
					log.error(e.getMessage(), e);
					return;
				}
				updateServerEntry(entry);
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
		
		trackSub.cmd(HaCommand.PubAll, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				List<ServerEntry> serverEntries = ServerEntryTable.parseJson(msg.getBodyString());
				for(ServerEntry entry : serverEntries){ 
					updateServerEntry(entry); 
				}
				syncFromTracker.countDown();
			}
		});
		
		trackSub.start();
		
		try {
			syncFromTracker.await(syncFromTrackerTimeout, TimeUnit.MILLISECONDS);
			log.debug("Synchronized from Tracker");
		} catch (InterruptedException e) { 
			//ignore
		}
		if(syncFromTracker.getCount() > 0){
			log.debug("Timeout synchronizing from Tracker");
		}
	}
	
	private void updateServerEntry(ServerEntry entry) throws IOException{
		boolean isNewServer = haServerEntrySet.isNewServer(entry); 
		haServerEntrySet.updateServerEntry(entry); 
		if(isNewServer){
			onNewServer(entry.serverAddr);
		}
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
	
	private Broker getBroker(String serverAddr){
		if(serverAddr == null) return null;
		return allBrokers.get(serverAddr);
	}

	
	
	@Override
	public Broker selectByClientHint(ClientHint hint) { 
		Broker broker = getBroker(hint.getServer());
		if(broker != null) return broker;
		
		if(hint.getEntry() != null){
			ServerList p = haServerEntrySet.getEntryServerList(hint.getEntry());
			if(p != null){
				ServerEntry e = p.iterator().next();
				broker = getBroker(e.serverAddr);
				if(broker != null) return broker;
			}
		} 
		
		return null;
	}

	@Override
	public String getEntry(Message msg) {
		return msg.getMq();
	}
	
	private List<Broker> defaultBroker(){
		Broker defaultBroker = allBrokers.values().iterator().next();
		if(defaultBroker == null){
			return null;
		}
		return Arrays.asList(defaultBroker);
	}
	
	@Override
	public List<Broker> selectByRequestMsg(Message msg) {  
		Broker broker = getBroker(msg.getServer());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		if(entry == null){
			return defaultBroker();
		}
		
		ServerList p = haServerEntrySet.getEntryServerList(entry);
		if(p == null || p.isEmpty()) return null;
		

		String mode = p.getMode();
		if(ServerEntry.PubSub.equals(mode)){
			List<Broker> res = new ArrayList<Broker>();
			for(ServerEntry e : p){ 
				broker = getBroker(e.serverAddr);
				if(broker != null){
					res.add(broker);
				}
			}
			return res;
		} 

		String serverAddr = p.iterator().next().serverAddr; //TODO 负载均衡算法
		broker = getBroker(serverAddr);
		if(broker != null){
			return Arrays.asList(broker);
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
		for(Broker broker : allBrokers.values()){
			broker.close();
		}
		allBrokers.clear();
		trackSub.close();
		haServerEntrySet.close();
		
		if(ownDispatcher){
			dispatcher.close();
		}
	}
	
	 
	public static void main(String[] args) throws Exception{
		BrokerConfig config = new BrokerConfig();
		config.setTrackServerList("127.0.0.1:16666");
		 
		HaBroker broker = new HaBroker(config);  
		broker.close();
	}
}


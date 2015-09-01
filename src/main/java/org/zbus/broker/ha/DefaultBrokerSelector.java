package org.zbus.broker.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.broker.ha.ServerEntryTable.ServerList;
import org.zbus.kit.NetKit;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.Message.MessageHandler;

public class DefaultBrokerSelector implements BrokerSelector{
	private static final Logger log = Logger.getLogger(DefaultBrokerSelector.class);
	private static final String localIp = NetKit.getLocalIp();
	
	private final ServerEntryTable serverEntryTable = new ServerEntryTable();
	private final Map<String, Broker> allBrokers = new ConcurrentHashMap<String, Broker>();
	
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
	
	
	
	private Broker getBroker(String serverAddr){
		if(serverAddr == null) return null;
		return allBrokers.get(serverAddr);
	}
	
	private Broker defaultBroker(){
		Iterator<Broker> iter = allBrokers.values().iterator();
		if(iter.hasNext()){
			return iter.next();
		} 
		return null;
	}
	
	private Broker getBrokerByIpCluster(){
		if(allBrokers.isEmpty()) return null;
		ArrayList<Broker> brokers = new ArrayList<Broker>(allBrokers.values());
		int idx = localIp.hashCode()%brokers.size();
		return brokers.get(idx);
	}
	
	@Override
	public String getEntry(Message msg) {
		return msg.getMq();
	} 
	
	/**
	 * 优先级顺序:
	 * 1) 指定Server
	 * 2) 指定Entry
	 */
	public Broker selectByBrokerHint(BrokerHint hint) { 
		//1) Server最高优先级
		Broker broker = getBroker(hint.getServer()); //优先级最高
		if(broker != null) return broker;
		
		//2) Entry次之
		String entryId = hint.getEntry(); 
		if(entryId!= null){
			ServerList p = serverEntryTable.getServerList(entryId); 
			if(p != null && !p.isEmpty()){ 
				ServerEntry se = p.msgFirstList.get(0);
				if(se.unconsumedMsgCount > 0){ //存在未能消费掉消息的Entry，优先选择
					broker = getBroker(se.serverAddr);
					if(broker != null) return broker;
				}
			}
		} 
		
		//最后使用默认按IP簇集
		return getBrokerByIpCluster();
	} 
	
	
	@Override
	public List<Broker> selectByRequestMsg(Message msg) {  
		Broker broker = getBroker(msg.getServer());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		if(entry == null){
			broker = defaultBroker();
			if(broker != null){
				return Arrays.asList(broker);
			} else {
				return null;
			}
		}
		
		ServerList p = serverEntryTable.getServerList(entry);
		if(p == null || p.isEmpty()){
			broker = defaultBroker();
			if(broker != null){
				return Arrays.asList(broker);
			} else {
				return null;
			}
		}
		

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
		
		int withConsumerCount = 0;
		Iterator<ServerEntry> iter = p.consumerFirstList.iterator();
		while(iter.hasNext()){
			ServerEntry se = iter.next();
			if(se.consumerCount > 0) withConsumerCount++;
		}
		if(withConsumerCount > 0){
			int idx = localIp.hashCode()%withConsumerCount;
			ServerEntry se = p.consumerFirstList.get(idx);
			if(se != null){
				broker = getBroker(se.serverAddr);
				if(broker != null){
					return Arrays.asList(broker);
				} 
			}
		} 
		
		broker = getBrokerByIpCluster();
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
		serverEntryTable.close();
		
		if(ownDispatcher){
			dispatcher.close();
		}
	}
	
	
	private void subscribeNotification(){
		trackSub = new TrackSub(config.getTrackServerList(), dispatcher);
		
		trackSub.cmd(HaCommand.ServerJoin, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				if(serverEntryTable.isNewServer(serverAddr)){
					onNewServer(serverAddr);
				}
			}
		});
		
		trackSub.cmd(HaCommand.ServerLeave, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				serverEntryTable.removeServer(serverAddr);
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
				serverEntryTable.removeServerEntry(serverAddr, entryId);
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
		boolean isNewServer = serverEntryTable.isNewServer(entry); 
		serverEntryTable.updateServerEntry(entry); 
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
}
package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.JsonKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.Client.MsgHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;

public class Broker implements Closeable { 
	private static final Logger log = LoggerFactory.getLogger(Broker.class);
	
	private Map<ServerAddress, MqClientPool> poolTable = new ConcurrentHashMap<ServerAddress, MqClientPool>();   
	private BrokerRouteTable routeTable = new BrokerRouteTable(); 
	private Map<ServerAddress, MessageClient> trackerSubscribers = new ConcurrentHashMap<ServerAddress, MessageClient>(); 
	
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>(); 
	private EventDriver eventDriver;  
	private final int clientPoolSize; 
	private Map<String, String> sslCertFileTable;
	private String defaultSslCertFile;
	
	private CountDownLatch ready = new CountDownLatch(1);
	private boolean waitCheck = true;
	
	public Broker(){
		this(new BrokerConfig());
	}
	
	public Broker(String configFile){
		this(new BrokerConfig(configFile));
	} 
	
	public Broker(BrokerConfig config){
		this.eventDriver = new EventDriver(); 
		this.clientPoolSize = config.getClientPoolSize();  
		this.sslCertFileTable = config.getSslCertFileTable();
		this.defaultSslCertFile = config.getDefaultSslCertFile(); 
		
		List<ServerAddress> trackerList = config.getTrackerList();
		
		int totalServerCount = queryServerCount(trackerList);
		if(totalServerCount > 1){
			ready = new CountDownLatch(totalServerCount);
		}
		for(ServerAddress serverAddress : trackerList){ 
			subscribeToTracker(serverAddress); 
		}  
		
		
		List<ServerAddress> serverList = config.getServerList();
		for(ServerAddress serverAddress : serverList){
			try {
				addServer(serverAddress);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}  
	 
	
	public MqClientPool[] selectClient(ServerSelector selector, String topic) {
		checkReady();
		
		ServerAddress[] serverList = selector.select(routeTable, topic);
		if(serverList == null){
			return poolTable.values().toArray(new MqClientPool[0]);
		}
		
		MqClientPool[] pools = new MqClientPool[serverList.length];
		int count = 0;
		for(int i=0; i<serverList.length; i++){
			pools[i] = poolTable.get(serverList[i]);
			if(pools[i] != null) count++;
		} 
		if(count == serverList.length) return pools;


		MqClientPool[] pools2 = new MqClientPool[count];
		int j = 0;
		for(int i=0; i<pools.length; i++){
			if(pools[i] != null){
				pools2[j++] = pools[i];
			}
		} 
		return pools2;
	}
 
	public MqClientPool selectClient(String serverAddress) {
		checkReady();
		return poolTable.get(serverAddress); 
	}  
	
	
	public void addSslCertFile(String address, String certPath){
		sslCertFileTable.put(address, certPath);
	}
	
	public void addTracker(String trackerAddress, String certPath){
		ServerAddress serverAddress = new ServerAddress(trackerAddress);
		serverAddress.sslEnabled = certPath != null;
		if(certPath != null){
			sslCertFileTable.put(serverAddress.address, certPath);
		} 
		
		addTracker(serverAddress);
	} 
	
	public void addTracker(String trackerAddress){
		addTracker(trackerAddress, null);
	} 
	
	public void addTracker(ServerAddress trackerAddress){
		if(trackerSubscribers.containsKey(trackerAddress)){
			return;
		} 
		subscribeToTracker(trackerAddress);  
	}
	
	public void addServer(String address) throws IOException { 
		addServer(address, null);
	}
	
	public void addServer(String address, String certFile) throws IOException { 
		ServerAddress serverAddress = new ServerAddress(address);
		if(certFile != null){
			serverAddress.sslEnabled = true;
			sslCertFileTable.put(address, certFile);
		}
		addServer(serverAddress);
	}
	 
	public void addServer(final ServerAddress serverAddress) throws IOException {  
		MqClient client = null;
		MqClientPool pool = null;
		ServerInfo serverInfo = null;
		try { 
			client = connectToServer(serverAddress);
			serverInfo = client.queryServer();  
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return;
		} finally {
			if(client != null){
				client.close();
			}
		}	
		final ServerAddress realServerAddress = serverInfo.serverAddress;
		synchronized (poolTable) {
			pool = poolTable.get(realServerAddress);
			if(pool != null) return; 
			 
			try{
				pool = createMqClientPool(realServerAddress, serverAddress);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return;
			}
			poolTable.put(realServerAddress, pool);  
			poolTable.put(serverAddress, pool); 
			
			
			pool.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					removeServer(realServerAddress);
				}
			});  
		}   
		routeTable.updateServer(serverInfo);
		ready.countDown();
			
		 
		final MqClientPool createdPool = pool;
		eventDriver.getGroup().submit(new Runnable() { 
			@Override
			public void run() {  
				try { 
					for(final ServerNotifyListener listener : listeners){
						eventDriver.getGroup().submit(new Runnable() { 
							@Override
							public void run() {  
								listener.onServerJoin(createdPool);
							}
						});
					} 
				} catch (Exception e) { 
					log.error(e.getMessage(), e);
				}  
			}
		}); 
	}  
	 
	public void removeServer(final ServerAddress serverAddress) { 
		final MqClientPool pool;
		synchronized (poolTable) { 
			pool = poolTable.remove(serverAddress);
			if(pool == null) return;
			
			routeTable.removeServer(serverAddress);  
			
			eventDriver.getGroup().schedule(new Runnable() {
				
				@Override
				public void run() {
					try {
						pool.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					} 
				}
			}, 1000, TimeUnit.MILLISECONDS); //delay 1s to close to wait other service depended on this broker
			
		}    
		
		for(final ServerNotifyListener listener : listeners){
			eventDriver.getGroup().submit(new Runnable() { 
				@Override
				public void run() { 
					listener.onServerLeave(serverAddress);
				}
			});
		}
	} 
	 
	public void addServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.add(listener);
	}
 
	public void removeServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.remove(listener);
	} 
	
	@Override
	public void close() throws IOException {
		for(MessageClient client : trackerSubscribers.values()){
			client.close();
		}
		trackerSubscribers.clear();
		synchronized (poolTable) {
			for(MqClientPool pool : poolTable.values()){ 
				pool.close();
			}
			poolTable.clear();
		}  
		
		eventDriver.close();
	} 
	
	private int queryServerCount(List<ServerAddress> serverAddressList){ 
		Set<ServerAddress> addrSet = new HashSet<ServerAddress>(); 
		for(ServerAddress serverAddress : serverAddressList){ 
			final MqClient client = connectToServer(serverAddress);
			try { 
				ServerInfo info = client.queryServer();
				addrSet.addAll(info.trackedServerList); 
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				try {
					client.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			} 
		}
		return addrSet.size(); 
	}
	
	private void subscribeToTracker(final ServerAddress serverAddress){
		final MessageClient client = connectToServer(serverAddress);
		final String localTrackerAddress = serverAddress.address;
		client.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				log.warn("Disconnected from tracker(%s)", serverAddress);  
				client.ensureConnectedAsync(); 
			}  
		});
		
		client.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected() throws IOException { 
				log.info("Connected to tracker(%s)", serverAddress);  
				
				Message req = new Message();  
				req.setCommand(Protocol.TRACK_SUB);
				req.setAck(false); 
				client.invokeAsync(req);
			}
		}); 
		
		client.onMessage(new MsgHandler<Message>() {  
			@Override
			public void handle(Message msg, Session session) throws IOException { 
				if(Protocol.TRACK_PUB.equals(msg.getCommand())){ 
					TrackerInfo trackerInfo = JsonKit.parseObject(msg.getBodyString(), TrackerInfo.class);
					
					//remote tracker's real address obtained, update ssl cert file mapping
					if(trackerInfo.serverAddress.sslEnabled){
						String sslCertFile = sslCertFileTable.get(localTrackerAddress);
						if(sslCertFile != null){
							sslCertFileTable.put(trackerInfo.serverAddress.address, sslCertFile);
						}
					}
					
					onTrackInfoUpdate(trackerInfo);
				}
			}
		});
		
		client.ensureConnectedAsync();
		trackerSubscribers.put(serverAddress, client); 
	}
	
	private void onTrackInfoUpdate(TrackerInfo trackerInfo) throws IOException {
		List<ServerAddress> toRemove = routeTable.updateVotes(trackerInfo);
		if(!toRemove.isEmpty()){ 
			for(ServerAddress serverAddress : toRemove){
				removeServer(serverAddress);
			}
		}
		
		for(ServerAddress serverAddress : trackerInfo.trackedServerList){
			MqClientPool pool = poolTable.get(serverAddress);
			if(pool == null){
				addServer(serverAddress); 
			}  
		}
	} 
	private void checkReady(){
		if(waitCheck){
			try {
				ready.await(3000, TimeUnit.MILLISECONDS); 
			} catch (InterruptedException e) {
				//ignore 
			}
			waitCheck = false;
		}
	}

	private MqClient connectToServer(ServerAddress serverAddress){
		EventDriver driver = eventDriver.duplicate(); //duplicated, no need to close
		if(serverAddress.sslEnabled){
			String certPath = sslCertFileTable.get(serverAddress.address);
			if(certPath == null) certPath = defaultSslCertFile;
			if(certPath == null){
				throw new IllegalStateException(serverAddress + " certificate file not found");
			}
			driver.setClientSslContext(certPath); 
		}
		
		final MqClient client = new MqClient(serverAddress.address, driver);  
		return client;
	}
	
	private MqClientPool createMqClientPool(ServerAddress remoteServerAddress, ServerAddress serverAddress){
		EventDriver driver = eventDriver.duplicate(); //duplicated, no need to close
		if(serverAddress.sslEnabled){
			String certPath = sslCertFileTable.get(remoteServerAddress.address);
			if(certPath == null) certPath = sslCertFileTable.get(serverAddress.address);
			if(certPath == null) certPath = defaultSslCertFile;
			
			if(certPath == null){
				throw new IllegalStateException(serverAddress + " certificate file not found");
			}
			driver.setClientSslContext(certPath);
		}
		return new MqClientPool(serverAddress.address, clientPoolSize, driver);   
	} 
	
	public void setDefaultSslCertFile(String defaultSslCertFile) {
		this.defaultSslCertFile = defaultSslCertFile;
	} 
	
	public static interface ServerSelector { 
		ServerAddress[] select(BrokerRouteTable table, String topic); 
	} 
	
	public static interface ServerNotifyListener { 
		void onServerJoin(MqClientPool server); 
		void onServerLeave(ServerAddress serverAddress);
	}	
}

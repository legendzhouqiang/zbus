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
	
	private Map<String, MqClientPool> poolMap = new ConcurrentHashMap<String, MqClientPool>();   
	private BrokerRouteTable routeTable = new BrokerRouteTable();
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>(); 
	private EventDriver eventDriver;  
	private final int clientPoolSize;
	
	private Map<String, MessageClient> trackSubscribers = new ConcurrentHashMap<String, MessageClient>(); 
	
	private CountDownLatch ready = new CountDownLatch(1);
	private boolean waitCheck = true;
	
	public Broker(String trackerAddress, int clientPoolSize){  
		this.eventDriver = new EventDriver();
		this.clientPoolSize = clientPoolSize;
		
		if(trackerAddress != null){
			String[] serverAddressList = trackerAddress.split("[;, ]");
			if(serverAddressList.length <= 0){
				throw new IllegalArgumentException("brokerAddress illegal: " + trackerAddress);
			}
			int totalServerCount = queryServerCount(serverAddressList);
			if(totalServerCount > 1){
				ready = new CountDownLatch(totalServerCount);
			}
			for(String serverAddress : serverAddressList){
				serverAddress = serverAddress.trim();
				if(serverAddress.isEmpty()) continue;
				subscribeToTracker(serverAddress); 
			} 
		}
	}
	
	public Broker(String trackerAddress){
		this(trackerAddress, 64);
	} 
	
	public Broker(int clientPoolSize){
		this(null, clientPoolSize);
	}
	
	public Broker(){
		this(null);
	}
	 
	public MqClientPool[] selectClient(ServerSelector selector, String topic) {
		checkReady();
		
		String[] serverList = selector.select(routeTable, topic);
		if(serverList == null){
			return poolMap.values().toArray(new MqClientPool[0]);
		}
		
		MqClientPool[] pools = new MqClientPool[serverList.length];
		int count = 0;
		for(int i=0; i<serverList.length; i++){
			pools[i] = poolMap.get(serverList[i]);
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
		return poolMap.get(serverAddress); 
	}  
	
	 
	public void addServer(String serverAddress) throws IOException {  
		MqClient client = null;
		MqClientPool pool = null;
		ServerInfo serverInfo = null;
		try {
			client = new MqClient(serverAddress, eventDriver);
			serverInfo = client.queryServer();  
		} catch (InterruptedException e) {
			return;
		} finally {
			if(client != null){
				client.close();
			}
		}	
		final String realServerAddress = serverInfo.serverAddress;
		synchronized (poolMap) {
			pool = poolMap.get(realServerAddress);
			if(pool != null) return; 
			
			pool = new MqClientPool(realServerAddress, clientPoolSize, eventDriver);  
			poolMap.put(realServerAddress, pool);
			pool.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					removeServer(realServerAddress);
				}
			});  
		}   
		routeTable.update(serverInfo);
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
	 
	public void removeServer(final String serverAddress) { 
		final MqClientPool pool;
		synchronized (poolMap) { 
			pool = poolMap.remove(serverAddress);
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
		for(MessageClient client : trackSubscribers.values()){
			client.close();
		}
		trackSubscribers.clear();
		synchronized (poolMap) {
			for(MqClientPool pool : poolMap.values()){ 
				pool.close();
			}
			poolMap.clear();
		}  
		
		eventDriver.close();
	} 
	
	private int queryServerCount(String... serverAddressList){ 
		Set<String> addrSet = new HashSet<String>(); 
		for(String address : serverAddressList){
			final MqClient client = new MqClient(address, eventDriver);  
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
	
	private void subscribeToTracker(final String serverAddress){
		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
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
					onTrackInfoUpdate(trackerInfo);
				}
			}
		});
		
		client.ensureConnectedAsync();
		trackSubscribers.put(serverAddress, client); 
	}
	
	private void onTrackInfoUpdate(TrackerInfo trackerInfo) throws IOException {
		List<String> toRemove = routeTable.updateVotes(trackerInfo);
		if(!toRemove.isEmpty()){ 
			for(String serverAddress : toRemove){
				removeServer(serverAddress);
			}
		}
		
		for(String serverAddress : trackerInfo.trackedServerList){
			MqClientPool pool = poolMap.get(serverAddress);
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


	public static interface ServerSelector { 
		String[] select(BrokerRouteTable table, String topic); 
	} 
	
	public static interface ServerNotifyListener { 
		void onServerJoin(MqClientPool server); 
		void onServerLeave(String serverAddress);
	}	
}

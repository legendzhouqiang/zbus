package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.kit.JsonKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
 

public class Tracker implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(Tracker.class);  
	 
	private Map<ServerAddress, MessageClient> downstreamTrackers = new ConcurrentHashMap<ServerAddress, MessageClient>();  
	private Map<ServerAddress, MessageClient> healthyUpstreamTrackers = new ConcurrentHashMap<ServerAddress, MessageClient>();
	private List<MessageClient> upstreamTrackers = new ArrayList<MessageClient>(); 
	
	private Set<Session> subscribers = new HashSet<Session>();
	
	private EventDriver eventDriver; 
	private final ServerAddress myServerAddress;
	private boolean myServerInTrack; 
	
	private Map<String, String> sslCertFileTable;

	
	public Tracker(ServerAddress myServerAddress, EventDriver eventDriver, 
			Map<String, String> sslCertFileTable, boolean myServerInTrack){ 
		this.myServerAddress = myServerAddress;
		this.eventDriver = eventDriver.duplicate(); 
		this.myServerInTrack = myServerInTrack; 
		this.sslCertFileTable = sslCertFileTable;
	} 
	 
	public TrackerInfo trackerInfo(){  
		List<ServerAddress> serverList = new ArrayList<ServerAddress>(this.downstreamTrackers.keySet()); 
		if(myServerInTrack){
			serverList.add(myServerAddress);
		}
		TrackerInfo trackerInfo = new TrackerInfo(); 
		trackerInfo.serverAddress = myServerAddress;
		trackerInfo.trackedServerList = serverList;
		return trackerInfo;
	}  
	
	public void joinUpstream(List<ServerAddress> trackerList){
		if(trackerList == null || trackerList.isEmpty()) return; 
		
    	for(final ServerAddress trackerAddress : trackerList){  
    		log.info("Connecting to Tracker(%s)", trackerAddress.toString());  
    		final MessageClient client = connectToServer(trackerAddress);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Disconnected from Tracker(%s)", trackerAddress.address);
					healthyUpstreamTrackers.remove(trackerAddress);
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Connected to Tracker(%s)", trackerAddress.address);
    				healthyUpstreamTrackers.put(trackerAddress, client);
    				ServerEvent event = new ServerEvent();
    				event.serverAddress = myServerAddress;
    				event.live = true;
    				notifyUpstream(client, event);
    			}
			});
    		upstreamTrackers.add(client);
    		client.ensureConnectedAsync();
    	}  
	}
	
	private MessageClient connectToServer(ServerAddress serverAddress){
		EventDriver driver = eventDriver.duplicate(); //duplicated, no need to close
		String certPath = sslCertFileTable.get(serverAddress.address);
		if(certPath != null){
			driver.setClientSslContext(certPath);
		}
		final MessageClient client = new MessageClient(serverAddress.address, driver);  
		return client;
	}
	
	private void notifyUpstream(MessageClient client, ServerEvent event){ 
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(event));  
		message.setAck(false); 
		
		try {  
			client.invokeAsync(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}    
	} 
	
	
	public void onDownstreamNotified(final ServerEvent event){  
		final ServerAddress serverAddress = event.serverAddress;
		if(myServerAddress.equals(serverAddress)){//myServer changes, just ignore
			return;
		}   
		
		if(event.live && !downstreamTrackers.containsKey(serverAddress)){ //new downstream tracker
			final MessageClient client = connectToServer(serverAddress);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Server(%s) lost of tracking", serverAddress);
					downstreamTrackers.remove(serverAddress);  
					publishToSubscribers();   
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Server(%s) in track", serverAddress);
    				downstreamTrackers.put(serverAddress, client);  
					publishToSubscribers();   
    			}
			});
    		try{
    			downstreamTrackers.put(serverAddress, client);
    			client.connectAsync();  //TODO handle failed connections
    		}catch (Exception e) {
				log.error(e.getMessage(), e); 
			}
    		
    		return;
		}
		
		if(!event.live){ //server down
			MessageClient downstreamTracker = downstreamTrackers.remove(serverAddress);
			if(downstreamTracker != null){
				try {
					downstreamTracker.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			} 
		} 
		
		publishToSubscribers();  
	}
	
	public void myServerChanged() {
		ServerEvent event = new ServerEvent();
		event.serverAddress = myServerAddress;
		event.live = true;
		
		for(MessageClient tracker : healthyUpstreamTrackers.values()){
			try{
				notifyUpstream(tracker, event);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}  
		publishToSubscribers();   
	}
	 
	 
	public void subscribe(Message msg, Session session){
		subscribers.add(session);  
		
		Message message = trackerInfoPubMessage();
		try {  
			session.writeAndFlush(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}   
	}  
	 
	public void publishToSubscribers(){
		if(subscribers.isEmpty()) return;
		
		Message message = trackerInfoPubMessage();
		for(Session session : subscribers){
			try{
				session.writeAndFlush(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	} 
	
	private Message trackerInfoPubMessage(){
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(trackerInfo()));
		message.setStatus(200);// server to client
		return message;
	}
	
	public void cleanSubscriberSession(Session session){
		if(subscribers.contains(session)){
			subscribers.remove(session);
		}
	} 
	
	@Override
	public void close() throws IOException {
		for(MessageClient client : upstreamTrackers){
			client.close();
		}
		upstreamTrackers.clear();
		for(MessageClient client : downstreamTrackers.values()){
			client.close();
		}
		downstreamTrackers.clear(); 
		subscribers.clear(); //No need to close
		
		eventDriver.close(); //duplicated, ok to close
	} 
}

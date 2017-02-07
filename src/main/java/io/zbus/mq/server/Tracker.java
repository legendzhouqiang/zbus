package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;
 
/**
 * Reporter == (ServerEvent) ==> Tracker ==> (Whole Table) ==> Subscriber
 * 
 * @author Rushmore
 *
 */
public class Tracker implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(Tracker.class);  
	
	private final String trackerList; 
	private Map<String, MessageClient> reporterMap = new ConcurrentHashMap<String, MessageClient>();  
	private Set<Session> subscribers = new HashSet<Session>();
	
	private Map<String, MessageClient> healthyTrackerMap = new ConcurrentHashMap<String, MessageClient>();
	private List<MessageClient> allTrackerList = new ArrayList<MessageClient>(); 
	
	private EventDriver eventDriver; 
	private final String thisServerAddress;
	private boolean thisServerIncluded; 

	
	public Tracker(MqServer mqServer, String trackerList, boolean thisServerIncluded){ 
		this.thisServerAddress = mqServer.getServerAddress();
		this.eventDriver = mqServer.getEventDriver();
		this.trackerList = trackerList;
		this.thisServerIncluded = thisServerIncluded; 
	} 
	
	public Tracker(MqServer mqServer, String trackerList){
		this(mqServer, trackerList, true); 
	}
	
	/**
	 * Trying to connect to Tracker(reside in MqServer)
	 */
	public void connect(){
		if(trackerList == null || trackerList.isEmpty()) return;
		
		log.info("Connecting to Tracker(%s)", this.trackerList); 
		String[] blocks = trackerList.split("[;]");
    	for(String block : blocks){
    		final String serverAddress = block.trim();
    		if(serverAddress.equals("")) continue;
    		 
    		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Disconnected from Tracker(%s)", serverAddress);
					healthyTrackerMap.remove(serverAddress);
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Connected to Tracker(%s)", serverAddress);
    				healthyTrackerMap.put(serverAddress, client);
    				ServerEvent event = new ServerEvent();
    				event.serverAddress = thisServerAddress;
    				event.live = true;
    				publishToTracker(client, event);
    			}
			});
    		allTrackerList.add(client);
    		client.ensureConnectedAsync();
    	}  
	}
	
	@Override
	public void close() throws IOException {
		for(MessageClient client : allTrackerList){
			client.close();
		}
		allTrackerList.clear();
		for(MessageClient client : reporterMap.values()){
			client.close();
		}
		reporterMap.clear();
		
		subscribers.clear(); //No need to close
	}
	 
	
	public TrackerInfo queryTrackerInfo(){  
		List<String> serverList = new ArrayList<String>(this.reporterMap.keySet()); 
		if(thisServerIncluded){
			serverList.add(thisServerAddress);
		}
		TrackerInfo trackerInfo = new TrackerInfo(); 
		trackerInfo.serverAddress = thisServerAddress;
		trackerInfo.liveServerList = serverList;
		return trackerInfo;
	}  
	
	private void publishToTracker(MessageClient client, ServerEvent event){ 
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonUtil.toJSONString(event));  
		message.setAck(false); 
		
		try {  
			client.invokeAsync(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}    
	} 
	
	
	public void publish(final ServerEvent event){  
		final String serverAddress = event.serverAddress;
		if(thisServerAddress.equals(serverAddress)){//thisServer changes 
			for(MessageClient tracker : healthyTrackerMap.values()){
				try{
					publishToTracker(tracker, event);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}  
			publishToSubscribers();  
			return;
		}   
		
		if(event.live && !reporterMap.containsKey(serverAddress)){ //new reporter
			final MessageClient client = new MessageClient(serverAddress, eventDriver);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Server(%s) lost of tracking", serverAddress);
					reporterMap.remove(serverAddress);  
					publishToSubscribers();   
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Server(%s) in track", serverAddress);
    				reporterMap.put(serverAddress, client);  
					publishToSubscribers();   
    			}
			});
    		try{
    			reporterMap.put(serverAddress, client);
    			client.connectAsync();  //TODO handle failed connections
    		}catch (Exception e) {
				log.error(e.getMessage(), e); 
			}
    		
    		return;
		}
		
		if(!event.live){ //server down
			MessageClient reporter = reporterMap.remove(serverAddress);
			if(reporter != null){
				try {
					reporter.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			} 
		} 
		
		publishToSubscribers();  
	}
	
	public void publish() {
		ServerEvent event = new ServerEvent();
		event.serverAddress = thisServerAddress;
		event.live = true;
		publish(event);
	}
	 
	 
	public void subscribe(Message msg, Session session){
		subscribers.add(session);  
		
		Message message = liveServerListMessage();
		try {  
			session.writeAndFlush(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}   
	}  
	
	/**
	 *  publish all available server list to subscriber
	 */
	private void publishToSubscribers(){
		if(subscribers.isEmpty()) return;
		
		Message message = liveServerListMessage();
		for(Session session : subscribers){
			try{
				session.writeAndFlush(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	} 
	private Message liveServerListMessage(){
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonUtil.toJSONString(queryTrackerInfo()));
		message.setStatus(200);// server to client
		return message;
	}
	
	public void cleanSession(Session session){
		if(subscribers.contains(session)){
			subscribers.remove(session);
		}
	}
}

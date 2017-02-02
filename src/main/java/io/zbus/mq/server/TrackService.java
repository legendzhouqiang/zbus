package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.TrackTable;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

/**
 * 
 * TODO Add 1) Timer to pull table from inbounds
 * 			2) Timer to push table to subscribers
 * @author Rushmore
 *
 */
public class TrackService implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(TrackService.class); 
	private Map<String, ServerInfo> serverMap = new ConcurrentHashMap<String, ServerInfo>();
	
	private EventDriver eventDriver;
	private final MqServer mqServer;
	private final String thisServerAddress;
	private boolean thisServerIncluded;
	
	private final String trackServerList;
	
	private Map<String, MessageClient> healthyInboundServerMap = new ConcurrentHashMap<String, MessageClient>(); 
	private Map<String, MessageClient> healthyOutboundServerMap = new ConcurrentHashMap<String, MessageClient>();
	private List<MessageClient> allOutboundServerList = new ArrayList<MessageClient>();
	private List<MessageClient> allInboundServerList = new ArrayList<MessageClient>();
	
	private Set<Session> subscribers = new HashSet<Session>();
	
	public TrackService(MqServer mqServer, String trackServerList, boolean thisServerIncluded){
		this.mqServer = mqServer;
		this.thisServerAddress = mqServer.getServerAddress();
		this.eventDriver = mqServer.getEventDriver();
		this.trackServerList = trackServerList;
		this.thisServerIncluded = thisServerIncluded;
	} 
	
	public TrackService(MqServer mqServer, String outboundServerAddressList){
		this(mqServer, outboundServerAddressList, true);
	}
	
	/**
	 * Trying to connect to trackServers(zbus mqserver)
	 */
	public void start(){
		if(trackServerList == null || trackServerList.isEmpty()) return;
		
		log.info("Connecting to tracker(%s)", this.trackServerList); 
		String[] blocks = trackServerList.split("[;]");
    	for(String block : blocks){
    		final String serverAddress = block.trim();
    		if(serverAddress.equals("")) continue;
    		 
    		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Disconnected from tracker(%s)", serverAddress);
					healthyOutboundServerMap.remove(serverAddress);
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Connected to tracker(%s)", serverAddress);
    				healthyOutboundServerMap.put(serverAddress, client);
    				//TODO 
    			}
			});
    		allOutboundServerList.add(client);
    		client.ensureConnectedAsync();
    	}  
	}
	
	@Override
	public void close() throws IOException {
		for(MessageClient client : allOutboundServerList){
			client.close();
		}
		allOutboundServerList.clear();
		for(MessageClient client : healthyInboundServerMap.values()){
			client.close();
		}
		healthyInboundServerMap.clear();
	}
	 
	
	public TrackTable queryTrackTable(){
		Map<String, ServerInfo> map = new HashMap<String, ServerInfo>(serverMap);
		if(thisServerIncluded){
			map.put(thisServerAddress, mqServer.getMqAdaptor().getServerInfo());
		}
		TrackTable table = new TrackTable();
		table.publisher = thisServerAddress;
		
		table.serverMap = map;
		return table;
	}
	 
	
	private void publishToSubscribers(TrackTable table){
		if(subscribers.isEmpty()) return;
		
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB); 
		message.setJsonBody(JsonUtil.toJSONString(table));
		message.setStatus(200);// server to client
		for(Session session : subscribers){
			try{
				session.writeAndFlush(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	}  
	 
	
	private void publishToOutboundServers(TrackTable table){//client to server
		if(healthyOutboundServerMap.isEmpty()){
			return;
		}
		 
		Message message = new Message();
		message.setCommand(Protocol.TRACK_PUB); 
		message.setJsonBody(JsonUtil.toJSONString(table));
		message.setAck(false);
		for(Entry<String, MessageClient> e : healthyOutboundServerMap.entrySet()){
			try { 
				if(e.getKey().equals(table.trigger)){ //ignore trigger
					continue;
				}
				
				MessageClient client = e.getValue();
				client.invokeAsync(message,(MessageCallback)null);
			} catch (IOException ex) { 
				log.error(ex.getMessage(), ex);
			} 
		}
	} 
  
	public void publish(TrackTable table){ 
		publishToOutboundServers(table);
		publishToSubscribers(table); 
		
		//TODO handle inbounds new added
	}
	
	public void publish(){ 
		final TrackTable table = queryTrackTable();
		table.trigger = thisServerAddress;
		
		publish(table);
	}
	 
	
	public void subscribe(Message msg, Session session){
		subscribers.add(session); 
		 
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB); 
		message.setJsonBody(JsonUtil.toJSONString(queryTrackTable())); 
		message.setStatus(200);
		message.setAck(false);
		
		try {  
			session.writeAndFlush(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}   
	} 
}

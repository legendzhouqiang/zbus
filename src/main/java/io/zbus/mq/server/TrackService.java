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
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackItem;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

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
		this.thisServerAddress = mqServer.getServerAddr();
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
    				
    				for(ServerInfo info : queryServerTable().values()){ 
    					publishToOutboundServer(client, info);
    				}
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
	
	
	public Map<String, ServerInfo> queryServerTable(){
		Map<String, ServerInfo> map = new HashMap<String, ServerInfo>(serverMap);
		if(thisServerIncluded){
			map.put(thisServerAddress, mqServer.getMqAdaptor().getServerInfo());
		}
		return map;
	}
	 
	
	private void publishToSubscribers(TrackItem info, String type){
		if(subscribers.isEmpty()) return;
		
		Message message = new Message();
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, type);
		message.setJsonBody(JsonUtil.toJSONString(info));
		for(Session session : subscribers){
			try{
				session.writeAndFlush(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	}  
	
	private void publishToOutboundServers(TrackItem info, String type){
		if(healthyOutboundServerMap.isEmpty()){
			return;
		}
		
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, type);
		message.setJsonBody(JsonUtil.toJSONString(info));
		message.setAck(false);
		for(Entry<String, MessageClient> e : healthyOutboundServerMap.entrySet()){
			try {
				String serverAddress = e.getKey();
				if(serverAddress.equals(info.serverAddress)){
					continue;
				}
				
				MessageClient client = e.getValue();
				client.invokeAsync(message,(MessageCallback)null);
			} catch (IOException ex) { 
				log.error(ex.getMessage(), ex);
			} 
		}
	}
	
	private void publishToSubscribers(final ServerInfo serverInfo){
		publishToSubscribers(serverInfo, Protocol.TRACK_SERVER);
	} 
	
	private void publishToSubscribers(final TopicInfo topicInfo){
		publishToSubscribers(topicInfo, Protocol.TRACK_TOPIC); 
	}   
	
	private void publishToOutboundServer(MessageClient client , ServerInfo info){ 
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, Protocol.TRACK_SERVER);
		message.setJsonBody(JsonUtil.toJSONString(info)); 
		message.setAck(false);
		try {  
			client.invokeAsync(message,(MessageCallback)null);
		} catch (IOException ex) { 
			log.error(ex.getMessage(), ex);
		}  
	}
	
	private void publishToOutboundServers(ServerInfo serverInfo){
		publishToOutboundServers(serverInfo, Protocol.TRACK_SERVER);
	}
	
	private void publishToOutboundServers(TopicInfo topicInfo){
		publishToOutboundServers(topicInfo, Protocol.TRACK_TOPIC);
	}
	
	
	private void serverRemove(final ServerInfo info) {
		//TODO filter duplicated updates
		if(info.live){
			throw new IllegalArgumentException("calling serverRemove, ServerInfo should not be live");
		}
		serverMap.remove(info.serverAddress);
		MessageClient client = healthyInboundServerMap.remove(info.serverAddress);
		if(client != null){
			try {
				client.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		publishToOutboundServers(info);
		publishToSubscribers(info);
	}
	
	private void serverUpdate(final ServerInfo info){
		//TODO filter duplicated updates
		if(!thisServerAddress.equals(info.serverAddress)){
			serverMap.put(info.serverAddress, info);
		} 
		publishToOutboundServers(info);
		publishToSubscribers(info); 
	} 
	
	private void topicRemove(final TopicInfo info) {
		if(info.live){
			throw new IllegalArgumentException("calling topicRemove, TopicInfo should not be live");
		}
		//TODO filter duplicated updates
		if(!thisServerAddress.equals(info.serverAddress)){ 
			ServerInfo serverInfo = serverMap.get(info.serverAddress);
			if(serverInfo == null){ 
				log.warn("server not in local table:" + info.serverAddress);
				return; //not recorded, ignore it
			}
			serverInfo.topicMap.put(info.topicName, info);
		} 
		
		publishToOutboundServers(info);
		publishToSubscribers(info); 
	}
	
	private void topicUpdate(final TopicInfo info){
		//TODO filter duplicated updates
		if(!thisServerAddress.equals(info.serverAddress)){
			ServerInfo serverInfo = serverMap.get(info.serverAddress);
			if(serverInfo == null){ 
				log.warn("server not in local table:" + info.serverAddress);
				return; //not recorded, ignore it
			}
			serverInfo.topicMap.put(info.topicName, info);
		} 
		publishToOutboundServers(info);
		publishToSubscribers(info); 
	}
	
	/**
	 * publish ServerInfo to all subscribers, update local route if new
	 * @param info
	 */
	public void publish(final ServerInfo info){ 
		if(info.serverAddress == null){
			info.serverAddress = thisServerAddress;
		}
		final String serverAddress = info.serverAddress; 
		
		if(!info.live){
			serverRemove(info);
			return;
		}
		
		if(!serverAddress.equals(thisServerAddress) && !serverMap.containsKey(serverAddress)){  
			//server join
			final MessageClient client = new MessageClient(serverAddress, eventDriver);  
			client.onConnected(new ConnectedHandler() {
				@Override
				public void onConnected() throws IOException {   
					log.info("server(%s) in track", serverAddress);
					healthyInboundServerMap.put(serverAddress, client);
					serverUpdate(info);
				}
			});  
			
			client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException {  
					log.warn("server(%s) lost track", serverAddress);
					info.live = false;
					serverRemove(info);
				}  
			}); 
			
			allInboundServerList.add(client);
			client.connectAsync(); //TODO clean client
			return;
		}  
		
		serverUpdate(info);
	}
	
	public void publish(final TopicInfo info){  
		if(info.serverAddress == null){
			info.serverAddress = thisServerAddress;
		}
		
		if(!info.live){
			topicRemove(info);
			return;
		} 
		
		topicUpdate(info);
	}
	
	public void publish(Message message, Session session){
		String type = message.getHeader(Protocol.TRACK_TYPE);
		if(Protocol.TRACK_SERVER.equals(type)){
			ServerInfo info = JsonUtil.parseObject(message.getBodyString(), ServerInfo.class);
			if(info.serverAddress == null){
				log.warn("missing server address, dicarded: " + message.getBodyString());
				return;
			}
			publish(info);
		} else {
			TopicInfo info = JsonUtil.parseObject(message.getBodyString(), TopicInfo.class);
			if(info.serverAddress == null){
				log.warn("missing server address, dicarded: " + message.getBodyString());
				return;
			}
			publish(info);
		} 
	}
	
	public void subscribe(Message message, Session session){
		subscribers.add(session);
	} 
}

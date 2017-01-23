package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
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
		
		log.info("Start tracking to " + this.trackServerList); 
		String[] blocks = trackServerList.split("[;]");
    	for(String block : blocks){
    		final String serverAddress = block.trim();
    		if(serverAddress.equals("")) continue;
    		 
    		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					healthyOutboundServerMap.remove(serverAddress);
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException {    
    				healthyOutboundServerMap.put(serverAddress, client);
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
	 
	
	private void publishToSubscribers(final Object info, String type){
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
	
	private void publishToOutboundServers(Object info, String type){
		if(healthyOutboundServerMap.isEmpty()){
			return;
		}
		
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, type);
		message.setJsonBody(JsonUtil.toJSONString(info));
		for(MessageClient client : healthyOutboundServerMap.values()){
			try {
				client.invokeAsync(message,(MessageCallback)null);
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			} 
		}
	}
	
	private void publishToSubscribers(final ServerInfo serverInfo){
		publishToSubscribers(serverInfo, Protocol.TRACK_SERVER);
	} 
	
	private void publishToSubscribers(final TopicInfo topicInfo){
		publishToSubscribers(topicInfo, Protocol.TRACK_TOPIC); 
	}   
	
	private void publishToOutboundServers(ServerInfo serverInfo){
		publishToOutboundServers(serverInfo, Protocol.TRACK_SERVER);
	}
	
	private void publishToOutboundServers(TopicInfo topicInfo){
		publishToOutboundServers(topicInfo, Protocol.TRACK_TOPIC);
	}
	
	
	private void serverRemove(final ServerInfo info) {
		//TODO filter duplicated updates
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
	 * @param serverInfo
	 */
	public void publish(final ServerInfo serverInfo){
		final String serverAddress = serverInfo.serverAddress; 
		
		if(!serverInfo.live){
			serverRemove(serverInfo);
			return;
		}
		
		if(!serverAddress.equals(thisServerAddress) && !serverMap.containsKey(serverAddress)){  
			//server join
			final MessageClient client = new MessageClient(serverAddress, eventDriver);  
			client.onConnected(new ConnectedHandler() {
				@Override
				public void onConnected() throws IOException {   
					healthyInboundServerMap.put(serverAddress, client);
					serverUpdate(serverInfo);
				}
			});  
			
			client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException {  
					serverRemove(serverInfo);
				}  
			}); 
			
			allInboundServerList.add(client);
			client.connectAsync(); //TODO clean client
			return;
		}  
		
		serverUpdate(serverInfo);
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
			publish(info);
		} else {
			TopicInfo info = JsonUtil.parseObject(message.getBodyString(), TopicInfo.class);
			publish(info);
		} 
	}
	
	public void subscribe(Message message, Session session){
		subscribers.add(session);
	} 
}

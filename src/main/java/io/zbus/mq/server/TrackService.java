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
	
	private final String outboundServerAddressList;
	
	private Map<String, MessageClient> healthyInboundServerMap = new ConcurrentHashMap<String, MessageClient>(); 
	private Map<String, MessageClient> healthyOutboundServerMap = new ConcurrentHashMap<String, MessageClient>();
	private List<MessageClient> allOutboundServerList = new ArrayList<MessageClient>();
	
	private Set<Session> subscribers = new HashSet<Session>();
	
	public TrackService(MqServer mqServer, String outboundServerAddressList, boolean thisServerIncluded){
		this.mqServer = mqServer;
		this.thisServerAddress = mqServer.getServerAddr();
		this.eventDriver = mqServer.getEventDriver();
		this.outboundServerAddressList = outboundServerAddressList;
		this.thisServerIncluded = thisServerIncluded;
	} 
	
	public TrackService(MqServer mqServer, String outboundServerAddressList){
		this(mqServer, outboundServerAddressList, true);
	}
	
	
	public void start(){
		if(outboundServerAddressList == null || outboundServerAddressList.isEmpty()) return;
		
		log.info("Start tracking to " + this.outboundServerAddressList); 
		String[] blocks = outboundServerAddressList.split("[;]");
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
		
	}
	
	public Map<String, ServerInfo> queryServerTable(){
		Map<String, ServerInfo> map = new HashMap<String, ServerInfo>(serverMap);
		if(thisServerIncluded){
			map.put(thisServerAddress, mqServer.getMqAdaptor().getServerInfo());
		}
		return map;
	}
	
	
	private void publishToSubscribers(final ServerInfo serverInfo){
		Message message = new Message();
		message.setHeader(Protocol.TRACK_TYPE, Protocol.TRACK_SERVER);
		message.setJsonBody(JsonUtil.toJSONString(serverInfo));
		
		for(Session session : subscribers){
			try{
				session.writeAndFlush(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	} 
	
	private void publishToOutboundServers(final ServerInfo serverInfo){
		Message message = new Message(); 
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, Protocol.TRACK_SERVER);
		message.setJsonBody(JsonUtil.toJSONString(serverInfo));
		
		for(MessageClient client : healthyOutboundServerMap.values()){
			try {
				client.invokeAsync(message,(MessageCallback)null);
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			} 
		}
	}
	
	private void serverLeave(final String serverAddress){
		serverMap.remove(serverAddress);
	}
	
	private void serverJoin(final ServerInfo serverInfo){
		serverMap.put(serverInfo.serverAddress, serverInfo);
	}
	
	/**
	 * publish ServerInfo to all subscribers, update local route if new
	 * @param serverInfo
	 */
	public void publish(final ServerInfo serverInfo){
		final String serverAddress = serverInfo.serverAddress; 
		
		if(!serverInfo.live){
			serverLeave(serverAddress);
			return;
		}
		
		if(!serverAddress.equals(thisServerAddress) && !serverMap.containsKey(serverAddress)){  
			final MessageClient client = new MessageClient(serverAddress, eventDriver);  
			client.onConnected(new ConnectedHandler() {
				@Override
				public void onConnected() throws IOException {   
					healthyInboundServerMap.put(serverAddress, client);
					serverJoin(serverInfo);
				}
			});  
			
			client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					healthyInboundServerMap.remove(serverAddress);
					serverLeave(serverAddress);
				}  
			}); 
			
			client.connectAsync(); //TODO clean client
			return;
		} 
		
		publishToOutboundServers(serverInfo);
		publishToSubscribers(serverInfo); 
	}
	
	public void publish(final TopicInfo topicInfo){
		
	}
	
	public void publish(Message message, Session session){
		
	}
	
	public void subscribe(Message message, Session session){
		subscribers.add(session);
	} 
}

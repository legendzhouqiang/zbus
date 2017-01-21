package io.zbus.mq.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.EventDriver;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class Tracker {
	private static final Logger log = LoggerFactory.getLogger(Tracker.class);
	 
	private Map<String, ServerInfo> serverMap = new ConcurrentHashMap<String, ServerInfo>();
	
	private EventDriver eventDriver;
	private final MqAdaptor mqAdaptor;
	private final String thisServerAddress;
	private Set<MessageClient> joinedServers = new HashSet<MessageClient>();
	
	public Tracker(MqAdaptor mqAdaptor, String serverAddress, EventDriver eventDriver){
		this.mqAdaptor = mqAdaptor;
		thisServerAddress = serverAddress;
		this.eventDriver = eventDriver;
	}  
	
	public Map<String, ServerInfo> buildServerTable(){
		Map<String, ServerInfo> map = new HashMap<String, ServerInfo>(serverMap);
		if(this.thisServerAddress != null){
			map.put(thisServerAddress, mqAdaptor.getServerInfo());
		}
		return map;
	} 
	
	public void update(final ServerInfo serverInfo){
		log.info("Update server: " + JsonUtil.toJSONString(serverInfo));
		final String serverAddress = serverInfo.serverAddress;
		if(serverMap.containsKey(serverAddress)){
			serverMap.put(serverInfo.serverAddress, serverInfo);
			return;
		}
		
		//TODO timer to check server liveness
		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
		client.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected() throws IOException {  
				log.info("Reporting Server(%s) in track", serverAddress);
				joinedServers.add(client);   
				serverMap.put(serverInfo.serverAddress, serverInfo);
			}
		});  
		
		client.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				joinedServers.remove(client);  
				
				serverMap.remove(serverAddress);
			}  
		}); 
		
		client.connectAsync();
	}
	
	public void update(TopicInfo topicInfo){
		log.info("Update topic: " + JsonUtil.toJSONString(topicInfo));
		
		ServerInfo serverInfo = serverMap.get(topicInfo.serverAddress);
		String serverAddress = topicInfo.serverAddress;
		if(serverInfo == null){
			serverInfo = new ServerInfo();
			serverInfo.serverAddress = serverAddress;
			serverInfo.topicMap.put(topicInfo.topicName, topicInfo); 
			update(serverInfo);
		} else {
			serverMap.put(serverAddress, serverInfo);
		} 
	}
	
	public void update(Message message){
		String type = message.getHeader(Protocol.TRACK_TYPE); 
		
		if(Protocol.TRACK_SERVER.equalsIgnoreCase(type)){
			ServerInfo serverInfo = JsonUtil.parseObject(message.getBodyString(), ServerInfo.class);
			update(serverInfo);
			return;
		}
		//default to TopicInfo
		TopicInfo topicInfo = JsonUtil.parseObject(message.getBodyString(), TopicInfo.class);
		update(topicInfo);  
	}
}

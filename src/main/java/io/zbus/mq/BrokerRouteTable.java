package io.zbus.mq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;

/**
 *  Route table based on the Topics info across ZbusServers.
 *  
 *  ServerTable: server address => ServerInfo
 *  TopicTable : topic string => list of server contains this topic(TopicInfo details)
 *  VoteTable  : topic string => list of server voted this topic
 *
 */
public class BrokerRouteTable {  
	//Server Address ==> Voted tracker list(
	private Map<ServerAddress, Set<ServerAddress>> votesTable = new ConcurrentHashMap<ServerAddress, Set<ServerAddress>>(); 
	//Topic ==> Server List(topic span across ZbusServers)
	private Map<String, List<TopicInfo>> topicTable = new ConcurrentHashMap<String, List<TopicInfo>>(); 
	//Server Address ==> ServerInfo
	private Map<ServerAddress, ServerInfo> serverTable = new ConcurrentHashMap<ServerAddress, ServerInfo>();
	 
	public Map<ServerAddress, ServerInfo> serverTable() {
		return serverTable;
	}
 
	public Map<String, List<TopicInfo>> topicTable() {
		return topicTable;
	}

	public ServerInfo serverInfo(ServerAddress serverAddress) {
		return serverTable.get(serverAddress);
	}

	public ServerInfo randomServerInfo() {
		if (serverTable.isEmpty())
			return null;

		List<ServerInfo> servers = new ArrayList<ServerInfo>(serverTable.values());
		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		return servers.get(r.nextInt(servers.size()));
	}
	
	public void removeServer(ServerAddress serverAddress){
		Map<ServerAddress, ServerInfo> serverTableLocal = new ConcurrentHashMap<ServerAddress, ServerInfo>(serverTable);
		ServerInfo serverInfo = serverTableLocal.remove(serverAddress);
		if(serverInfo == null) return;
		
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable(serverTableLocal); 
		serverTable = serverTableLocal;
		topicTable = topicTableLocal;
	}

	public void updateServer(ServerInfo serverInfo) { 
		Map<ServerAddress, ServerInfo> serverTableLocal = new ConcurrentHashMap<ServerAddress, ServerInfo>(serverTable);
		serverTableLocal.put(serverInfo.serverAddress, serverInfo);
		
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable(serverTableLocal); 
		serverTable = serverTableLocal;
		topicTable = topicTableLocal;
	}  
	
	
	public List<ServerAddress> updateTrackerInfo(TrackerInfo trackerInfo){ 
		Map<ServerAddress, Set<ServerAddress>> votesTableLocal = new ConcurrentHashMap<ServerAddress, Set<ServerAddress>>(votesTable);
		
		List<ServerAddress> toRemove = new ArrayList<ServerAddress>();
		Iterator<Entry<ServerAddress, Set<ServerAddress>>> iter = votesTableLocal.entrySet().iterator();
		while(iter.hasNext()){
			Entry<ServerAddress, Set<ServerAddress>> e = iter.next();
			ServerAddress serverAddress = e.getKey();
			Set<ServerAddress> votedTrackerList = e.getValue(); 
			 
			if(!trackerInfo.trackedServerList.contains(serverAddress)){ 
				votedTrackerList.remove(trackerInfo.serverAddress);
			} 
			
			if(canRemove(votedTrackerList)){
				iter.remove();
				toRemove.add(serverAddress);
			}
		} 
		if(toRemove.isEmpty()) return toRemove;
		
		Map<ServerAddress, ServerInfo> serverTableLocal = new ConcurrentHashMap<ServerAddress, ServerInfo>(serverTable); 
		for(ServerAddress server : toRemove){
			serverTableLocal.remove(server);
		}
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable(serverTableLocal); 
		votesTable = votesTableLocal;
		serverTable = serverTableLocal;
		topicTable = topicTableLocal;
		
		return toRemove; 
	}
	
	private boolean canRemove(Set<ServerAddress> votes){
		return votes.isEmpty();
	}

	private Map<String, List<TopicInfo>> rebuildTopicTable(Map<ServerAddress, ServerInfo> serverMapLocal) {
		Map<String, List<TopicInfo>> table = new ConcurrentHashMap<String, List<TopicInfo>>();
		for (ServerInfo serverInfo : serverMapLocal.values()) {
			for (TopicInfo topicInfo : serverInfo.topicTable.values()) {
				List<TopicInfo> topicServerList = table.get(topicInfo.topicName);
				if (topicServerList == null) {
					topicServerList = new ArrayList<TopicInfo>();
					table.put(topicInfo.topicName, topicServerList);
				}
				topicServerList.add(topicInfo);
			}
		}
		return table;
	}
}
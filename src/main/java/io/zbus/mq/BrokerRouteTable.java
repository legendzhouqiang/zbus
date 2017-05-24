package io.zbus.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;
 
public class BrokerRouteTable {  
	//Server Address ==> Voted tracker list
	private Map<ServerAddress, Set<ServerAddress>> votesTable = new ConcurrentHashMap<ServerAddress, Set<ServerAddress>>(); 
	//Topic ==> Server List(topic span across ZbusServers)
	private Map<String, List<TopicInfo>> topicTable2 = new ConcurrentHashMap<String, List<TopicInfo>>();  
	 
	public Map<ServerAddress, ServerInfo> serverTable() {
		return serverTable;
	}
 
	public Map<String, List<TopicInfo>> topicTable() {
		return topicTable2;
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
		
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable2(serverTableLocal); 
		serverTable = serverTableLocal;
		topicTable2 = topicTableLocal;
	}

	public void updateServer(ServerInfo serverInfo) { 
		Map<ServerAddress, ServerInfo> serverTableLocal = new ConcurrentHashMap<ServerAddress, ServerInfo>(serverTable);
		serverTableLocal.put(serverInfo.serverAddress, serverInfo);
		
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable2(serverTableLocal); 
		serverTable = serverTableLocal;
		topicTable2 = topicTableLocal;
	}  
	
	
	public List<ServerAddress> updateVotes(TrackerInfo trackerInfo){ 
		Map<ServerAddress, Set<ServerAddress>> votesTableLocal = new ConcurrentHashMap<ServerAddress, Set<ServerAddress>>(votesTable);
		for(ServerAddress serverAddress : trackerInfo.trackedServerList){
			Set<ServerAddress> votedTrackerSet = votesTableLocal.get(serverAddress);
			if(votedTrackerSet == null){
				votedTrackerSet = new HashSet<ServerAddress>();
				votesTableLocal.put(serverAddress, votedTrackerSet);
			}
			votedTrackerSet.add(trackerInfo.serverAddress);
		}
		
		List<ServerAddress> toRemove = new ArrayList<ServerAddress>(); 
		Iterator<Entry<ServerAddress, Set<ServerAddress>>> iter = votesTableLocal.entrySet().iterator();
		while(iter.hasNext()){
			Entry<ServerAddress, Set<ServerAddress>> e = iter.next();
			ServerAddress serverAddress = e.getKey();
			Set<ServerAddress> votedTrackerSet = e.getValue(); 
			 
			if(!trackerInfo.trackedServerList.contains(serverAddress)){ 
				votedTrackerSet.remove(trackerInfo.serverAddress);
			} 
			
			if(canRemove(votedTrackerSet)){
				iter.remove();
				toRemove.add(serverAddress);
			}
		} 
		votesTable = votesTableLocal;
		if(toRemove.isEmpty()) return toRemove;
		
		Map<ServerAddress, ServerInfo> serverTableLocal = new ConcurrentHashMap<ServerAddress, ServerInfo>(serverTable); 
		for(ServerAddress server : toRemove){
			serverTableLocal.remove(server);
		}
		Map<String, List<TopicInfo>> topicTableLocal = rebuildTopicTable2(serverTableLocal); 
		
		serverTable = serverTableLocal;
		topicTable2 = topicTableLocal;
		
		return toRemove; 
	}
	
	private boolean canRemove(Set<ServerAddress> votes){
		return votes.isEmpty();
	}

	private Map<String, List<TopicInfo>> rebuildTopicTable2(Map<ServerAddress, ServerInfo> serverMapLocal) {
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
	
	
	private double voteFactor = 0.5;
	//Topic ==> Server List(topic span across ZbusServers)
	private Map<String, Map<ServerAddress, TopicInfo>> topicTable = new HashMap<String, Map<ServerAddress, TopicInfo>>(); 
	//Server Address ==> ServerInfo
	private Map<ServerAddress, ServerInfo> serverTable = new HashMap<ServerAddress, ServerInfo>();
	
	
	public List<ServerAddress> merge(TrackerInfo trackerInfo){ 
		Set<ServerAddress> trackedServerSet = new HashSet<ServerAddress>(trackerInfo.trackedServerList);
		
		Map<ServerAddress, ServerInfo> serverTableLocal = new HashMap<ServerAddress, ServerInfo>(serverTable);
		for(ServerInfo serverInfo : trackerInfo.serverTable.values()){
			serverTableLocal.put(serverInfo.serverAddress, serverInfo);
		}
		Map<String, Map<ServerAddress, TopicInfo>> topicTableLocal = rebuildTopicTable(serverTableLocal);
		
		serverTable = serverTableLocal;
		topicTable = topicTableLocal;
		return null;
	}
	
	private Map<String, Map<ServerAddress, TopicInfo>> rebuildTopicTable(Map<ServerAddress, ServerInfo> serverTableLocal) {
		Map<String, Map<ServerAddress, TopicInfo>> table = new ConcurrentHashMap<String, Map<ServerAddress, TopicInfo>>();
		for (ServerInfo serverInfo : serverTableLocal.values()) {
			for (TopicInfo topicInfo : serverInfo.topicTable.values()) {
				Map<ServerAddress, TopicInfo> server2Topic = table.get(topicInfo.topicName);
				if (server2Topic == null) {
					server2Topic = new HashMap<ServerAddress, TopicInfo>();
					table.put(topicInfo.topicName, server2Topic);
				}
				server2Topic.put(topicInfo.serverAddress, topicInfo);
			}
		}
		return table;
	} 
}
package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;

public interface Broker extends Closeable {

	MessageInvoker selectForProducer(String topic) throws IOException;

	MessageInvoker selectForConsumer(String topic) throws IOException;

	void releaseInvoker(MessageInvoker invoker) throws IOException;

	List<Broker> availableServerList();

	void setServerSelector(ServerSelector selector);

	void registerServer(String serverAddress) throws IOException;

	void unregisterServer(String serverAddress) throws IOException;

	void addServerListener(ServerNotifyListener listener);

	void removeServerListener(ServerNotifyListener listener);
	
	String brokerAddress();

	public static interface ServerSelector {
		String selectForProducer(RouteTable table, String topic);

		String selectForConsumer(RouteTable table, String topic);
	}

	public static interface ServerNotifyListener {
		void onServerJoin(Broker broker);

		void onServerLeave(String serverAddress);
	}

	public static class RouteTable {  
		private Map<String, Set<String>> votesMap = new ConcurrentHashMap<String, Set<String>>();
		
		private Map<String, ServerInfo> serverMap = new ConcurrentHashMap<String, ServerInfo>();
		private Map<String, List<TopicInfo>> topicTable = new ConcurrentHashMap<String, List<TopicInfo>>(); 
		
		public Map<String, ServerInfo> serverMap() {
			return serverMap;
		}

		public Map<String, List<TopicInfo>> topicTable() {
			return topicTable;
		}

		public ServerInfo serverInfo(String serverAddress) {
			return serverMap.get(serverAddress);
		}

		public ServerInfo randomServerInfo() {
			if (serverMap.isEmpty())
				return null;

			List<ServerInfo> servers = new ArrayList<ServerInfo>(serverMap.values());
			return servers.get(new Random().nextInt(servers.size()));
		}
		
		public void removeServer(String serverAddress){
			Map<String, ServerInfo> serverMapLocal = new ConcurrentHashMap<String, ServerInfo>(serverMap);
			ServerInfo serverInfo = serverMapLocal.remove(serverAddress);
			if(serverInfo == null) return;
			
			Map<String, List<TopicInfo>> topicTableLocal = rebuildTable(serverMapLocal); 
			serverMap = serverMapLocal;
			topicTable = topicTableLocal;
		}

		public void update(ServerInfo serverInfo) { 
			Map<String, ServerInfo> serverMapLocal = new ConcurrentHashMap<String, ServerInfo>(serverMap);
			serverMapLocal.put(serverInfo.serverAddress, serverInfo);
			
			Map<String, List<TopicInfo>> topicTableLocal = rebuildTable(serverMapLocal); 
			serverMap = serverMapLocal;
			topicTable = topicTableLocal;
		} 
		
		private boolean canRemove(Set<String> votes){
			return votes.isEmpty();
		}
		
		public List<String> updateVotes(TrackerInfo trackerInfo){ 
			Map<String, Set<String>> votesMapLocal = new ConcurrentHashMap<String, Set<String>>(votesMap);
			
			List<String> toRemove = new ArrayList<String>();
			Iterator<Entry<String, Set<String>>> iter = votesMapLocal.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String, Set<String>> e = iter.next();
				
				if(!trackerInfo.liveServerList.contains(e.getKey())){
					e.getValue().remove(trackerInfo.serverAddress);
				} 
				
				if(canRemove(e.getValue())){
					iter.remove();
					toRemove.add(e.getKey());
				}
			} 
			if(toRemove.isEmpty()) return toRemove;
			
			Map<String, ServerInfo> serverMapLocal = new ConcurrentHashMap<String, ServerInfo>(serverMap); 
			for(String server : toRemove){
				serverMapLocal.remove(server);
			}
			Map<String, List<TopicInfo>> topicTableLocal = rebuildTable(serverMapLocal); 
			votesMap = votesMapLocal;
			serverMap = serverMapLocal;
			topicTable = topicTableLocal;
			
			return toRemove; 
		}

		private Map<String, List<TopicInfo>> rebuildTable(Map<String, ServerInfo> serverMapLocal) {
			Map<String, List<TopicInfo>> table = new ConcurrentHashMap<String, List<TopicInfo>>();
			for (ServerInfo serverInfo : serverMapLocal.values()) {
				for (TopicInfo topicInfo : serverInfo.topicMap.values()) {
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
}

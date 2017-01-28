package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;

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
		private Map<String, ServerInfo> serverMap = new ConcurrentHashMap<String, ServerInfo>();
		private Map<String, List<TopicInfo>> topicTable = new ConcurrentHashMap<String, List<TopicInfo>>();
		private Random random = new Random();

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
			return servers.get(random.nextInt(servers.size()));
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
			if(!serverInfo.live){
				serverMapLocal.remove(serverInfo.serverAddress);
			} else {
				serverMapLocal.put(serverInfo.serverAddress, serverInfo);
			}
			
			Map<String, List<TopicInfo>> topicTableLocal = rebuildTable(serverMapLocal); 
			serverMap = serverMapLocal;
			topicTable = topicTableLocal;
		}

		public void update(TopicInfo topicInfo) {
			ServerInfo serverInfo = serverMap.get(topicInfo.serverAddress);
			if (serverInfo == null) {
				return;
			} 
			
			ServerInfo updatedServerInfo = serverInfo.clone();
			Map<String, ServerInfo> serverMapLocal = new ConcurrentHashMap<String, ServerInfo>(serverMap);
			
			if (!topicInfo.live) { // to remove  
				updatedServerInfo.topicMap.remove(topicInfo.topicName); 
 
			} else { 
				updatedServerInfo.topicMap.put(topicInfo.topicName, topicInfo);
			}
 
			serverMapLocal.put(serverInfo.serverAddress, updatedServerInfo);
			Map<String, List<TopicInfo>> topicTableLocal = rebuildTable(serverMapLocal);

			serverMap = serverMapLocal;
			topicTable = topicTableLocal;
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

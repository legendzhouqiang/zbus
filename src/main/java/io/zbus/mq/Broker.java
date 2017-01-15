package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public interface Broker extends Closeable{  
	
	MessageInvoker selectForProducer(String topic) throws IOException; 
	
	MessageInvoker selectForConsumer(String topic) throws IOException;  
	
	void releaseInvoker(MessageInvoker invoker) throws IOException; 
	
	List<Broker> availableServerList();
	
	void setServerSelector(ServerSelector selector);
	
	void registerServer(String serverAddress);
	
	void unregisterServer(String serverAddress);
	
	void addServerListener(ServerNotifyListener listener);
	
	void removeServerListener(ServerNotifyListener listener); 
	
	
	public static interface ServerSelector {
		String selectForProducer(ServerTable table, String topic);
		String selectForConsumer(ServerTable table, String topic);
	}
	
	public static interface ServerNotifyListener{
		
	} 
	
	public static class ServerTable{  
		public List<String> activeServerList = new ArrayList<String>();
		public Map<String, List<TopicInfo>> topicMap = new ConcurrentHashMap<String, List<TopicInfo>>();
	}
	
	public static class ServerInfo{
		public String serverAddress;
		public Map<String, TopicInfo> topicTable;
	}
	
	public static class TopicInfo {
		public String serverAddress;
		public String topicName;
		public long messageCount;
		public int consumerCount;
		public long lastUpdated; 
		
		public List<ConsumeGroupInfo> consumeGroups;
	} 
	
	public static class ConsumeGroupInfo {
		public String name;
		public long messageCount;
		public int consumerCount;
		public long lastUpdated;
	}
}

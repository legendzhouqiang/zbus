package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.TopicInfo;


public interface Broker extends Closeable{  
	
	MessageInvoker selectForProducer(String topic) throws IOException; 
	
	MessageInvoker selectForConsumer(String topic) throws IOException;  
	
	void releaseInvoker(MessageInvoker invoker) throws IOException; 
	
	List<Broker> availableServerList();
	
	void setServerSelector(ServerSelector selector);
	
	void registerServer(String serverAddress) throws IOException;
	
	void unregisterServer(String serverAddress) throws IOException;
	
	void addServerListener(ServerNotifyListener listener);
	
	void removeServerListener(ServerNotifyListener listener); 
	
	public static interface ServerSelector {
		String selectForProducer(RouteTable table, String topic);
		String selectForConsumer(RouteTable table, String topic);
	}
	
	public static interface ServerNotifyListener{
		void onServerJoin(String serverAddress, Broker broker);
		void onServerLeave(String serverAddress);
	} 
	
	public static class RouteTable{  
		public List<String> serverList = new ArrayList<String>();
		public Map<String, List<TopicInfo>> topicTable = new ConcurrentHashMap<String, List<TopicInfo>>();
	} 
}

package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zbus.log.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ServerEntryTable implements Closeable{
	private static final Logger log = Logger.getLogger(ServerEntryTable.class);
	//entry_id ==> list of same entries from different target_servers
	private Map<String, ServerList> entry2ServerList = new ConcurrentHashMap<String, ServerList>();
	//server_addr ==> list of entries from same target server
	private Map<String, Set<ServerEntry>> server2EntryList = new ConcurrentHashMap<String, Set<ServerEntry>>(); 

	private final ScheduledExecutorService dumpExecutor = Executors.newSingleThreadScheduledExecutor();
	
	public boolean verbose = true;
	
	public ServerEntryTable(){
		dumpExecutor.scheduleAtFixedRate(new Runnable() { 
			@Override
			public void run() { 
				if(verbose) dump();
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
	} 
	
	public void dump(){
		System.out.format("===============ServerEntryTable(%s)===============\n", new Date());
		Iterator<Entry<String, ServerList>> iter = entry2ServerList.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, ServerList> e = iter.next();
			System.out.format("%s\n", e.getKey());
			for(ServerEntry se : e.getValue()){
				System.out.format("  %s\n", se);
			} 
		}
		System.out.println("----------------------------------------------------------------------------");
		for(Entry<String, Set<ServerEntry>> e : server2EntryList.entrySet()){
			System.out.format("Server: %s\n", e.getKey());
			for(ServerEntry se : e.getValue()){
				System.out.format("  %s\n", se);
			} 
		}
		System.out.println();
	}
	
	public ServerList getServerList(String entryId){
		return entry2ServerList.get(entryId);
	}
	
	public boolean isNewServer(String serverAddr){
		return !server2EntryList.containsKey(serverAddr);
	}
	
	public boolean isNewServer(ServerEntry be){
		return isNewServer(be.serverAddr);
	}
	
	public void updateServerEntry(ServerEntry se){ 
		synchronized (entry2ServerList) {
			String entryId = se.entryId;
			ServerList serverList = entry2ServerList.get(entryId);
			if(serverList == null){
				serverList = new ServerList(entryId);
				entry2ServerList.put(entryId, serverList);
			} 
			serverList.updateServerEntry(se);
		}
		
		synchronized (server2EntryList) {
			String serverAddr = se.serverAddr;
			Set<ServerEntry> entryList = server2EntryList.get(serverAddr);
			if(entryList == null){
				entryList = Collections.synchronizedSet(new HashSet<ServerEntry>());
				server2EntryList.put(serverAddr, entryList);
			}
			entryList.remove(se);
			entryList.add(se);
		} 
	}
	
	
	public void removeServer(String serverAddr){ 
		server2EntryList.remove(serverAddr);
		
		Iterator<Entry<String, ServerList>> entryIter = entry2ServerList.entrySet().iterator();
		while(entryIter.hasNext()){
			Entry<String, ServerList> e = entryIter.next();
			ServerList serverList = e.getValue();  
			Iterator<ServerEntry> seIter = serverList.iterator();
			while(seIter.hasNext()){
				ServerEntry se = seIter.next();
				if(se.serverAddr.equals(serverAddr)){
					seIter.remove();
				}
			}
			if(serverList.isEmpty()){
				entryIter.remove();
			}
		} 
	}
	
	public void removeServerEntry(String serverAddr, String entryId){
		if(serverAddr == null || entryId == null) return;
		
		ServerList serverList = entry2ServerList.get(entryId);
		if(serverList == null) return;
		
		Iterator<ServerEntry> iter = serverList.iterator();
		while(iter.hasNext()){
			ServerEntry se = iter.next();
			if(se.serverAddr.equals(serverAddr)){
				iter.remove();
			}
		}
		
		Set<ServerEntry> entryList = server2EntryList.get(serverAddr);
		if(entryList == null) return;
		
		iter = entryList.iterator();
		while(iter.hasNext()){
			ServerEntry se = iter.next();
			if(se.entryId.equals(entryId)){
				iter.remove();
			}
		}
	}  
	
	public String toJsonString(){
		List<ServerEntry> entries = new ArrayList<ServerEntry>();
		for(Set<ServerEntry> entryList : server2EntryList.values()){
			entries.addAll(entryList);
		} 
		return JSON.toJSONString(entries);
	}
	 
	public static List<ServerEntry> parseJson(String jsonString){
		JSONArray jsonArray = JSON.parseArray(jsonString);
		List<ServerEntry> res = new ArrayList<ServerEntry>();
		for(Object obj : jsonArray){
			JSONObject json = (JSONObject)obj;
			ServerEntry entry = JSON.toJavaObject(json, ServerEntry.class);
			res.add(entry);
		}
		return res;
	} 
	
	@Override
	public void close() throws IOException {  
		dumpExecutor.shutdown(); 
	} 
	
	
	public static class ServerList implements Iterable<ServerEntry>{
		public final String entryId;
		public Map<String, ServerEntry> serverTable = new ConcurrentHashMap<String, ServerEntry>();		
		public List<ServerEntry> consumerFirstList = Collections.synchronizedList(new ArrayList<ServerEntry>());
		public List<ServerEntry> msgFirstList = Collections.synchronizedList(new ArrayList<ServerEntry>());
		
		private static final Comparator<ServerEntry> consumerFirst = new ConsumerFirstComparator();
		private static final Comparator<ServerEntry> msgFirst = new MsgFirstComparator();
		
		public ServerList(String entryId){
			this.entryId = entryId;
		}
		
		public String getMode(){ 
			if(consumerFirstList.isEmpty()) return null;
			return consumerFirstList.get(0).mode;
		}
		
		public void updateServerEntry(ServerEntry se){
			if(!se.entryId.equals(entryId)) return;
			
			serverTable.put(se.serverAddr, se);
			if(!consumerFirstList.remove(se)){
				log.info("Added: %s", se);
			}
			msgFirstList.remove(se);
			
			consumerFirstList.add(se); 
			msgFirstList.add(se);
			
			Collections.sort(consumerFirstList, consumerFirst);
			Collections.sort(msgFirstList, msgFirst);
		}
		
		public void removeServer(String serverAddr){
			serverTable.remove(serverAddr);
			
			Iterator<ServerEntry> iter = consumerFirstList.iterator();
			while(iter.hasNext()){
				ServerEntry se = iter.next();
				if(se.serverAddr.equals(serverAddr)){
					iter.remove();
				}
			}
			iter = msgFirstList.iterator();
			while(iter.hasNext()){
				ServerEntry se = iter.next();
				if(se.serverAddr.equals(serverAddr)){
					iter.remove();
				}
			}
			 
		}
		
		public Iterator<ServerEntry> iterator(){
			return consumerFirstList.iterator();
		}
		
		public boolean isEmpty(){
			return consumerFirstList.isEmpty();
		} 
		
		static class ConsumerFirstComparator implements Comparator<ServerEntry>{
			@Override
			public int compare(ServerEntry se1, ServerEntry se2) { 
				int rc = se2.consumerCount - se1.consumerCount;
				if(rc != 0) return rc;
				return (int)(se2.unconsumedMsgCount - se1.unconsumedMsgCount);
			} 
		}
		
		static class MsgFirstComparator implements Comparator<ServerEntry>{
			@Override
			public int compare(ServerEntry se1, ServerEntry se2) { 
				long rc = se2.unconsumedMsgCount - se1.unconsumedMsgCount;
				if(rc > 0) return 1;
				if(rc < 0) return -1;
				return se1.consumerCount - se2.consumerCount;
			} 
		}
	} 
}
/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.broker.ha;

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

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;

public class ServerEntryTable {
	private static final Logger log = LoggerFactory.getLogger(ServerEntryTable.class);
	//entry_id ==> list of same entries from different target_servers
	public Map<String, ServerList> entry2ServerList = new ConcurrentHashMap<String, ServerList>();
	//server_addr ==> list of entries from same target server
	public Map<String, Set<ServerEntry>> server2EntryList = new ConcurrentHashMap<String, Set<ServerEntry>>(); 
	 
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
	
	public void addServer(String serverAddr){
		synchronized (server2EntryList) {
			if(server2EntryList.containsKey(serverAddr)) return;
			
			Set<ServerEntry> entryList = Collections.synchronizedSet(new HashSet<ServerEntry>());
			server2EntryList.put(serverAddr, entryList);
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
	
	public Set<String> serverSet(){
		return server2EntryList.keySet();
	}
	
	public String pack(){ 
		StringBuilder sb = new StringBuilder(); 
		for(String server : server2EntryList.keySet()){
			sb.append(server + "\n");
		}
		sb.append("\r\n");
		for(Set<ServerEntry> entryList : server2EntryList.values()){
			for(ServerEntry se : entryList){
				sb.append(se.pack() + "\n");
			}
		} 
		if(sb.length() > 0){ //remove last \n
			sb.setLength(sb.length()-1);
		}
		return sb.toString();
	}
	
	public static ServerEntryTable unpack(String packedString){
		ServerEntryTable table = new ServerEntryTable(); 
		String[] bb = packedString.split("\r\n");
		if(bb.length > 0){ 
			String[] ss = bb[0].split("[\n]");
			for(String s : ss){
				s = s.trim();
				if(s.length() == 0) continue;
				table.addServer(s);
			} 
			if(bb.length > 1){
				ss = bb[1].split("[\n]"); 
				for(String s : ss){
					s = s.trim();
					if(s.length() == 0) continue;
					ServerEntry se = ServerEntry.unpack(s);
					if(se != null){
						table.updateServerEntry(se);
					}
				}
			}
		} 
		return table;
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
		
		public int getMode(){ 
			if(consumerFirstList.isEmpty()) return -1;
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
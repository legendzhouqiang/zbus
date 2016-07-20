package org.zbus.mq.server.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.http.Message;

public class MemoryMqFilter implements MqFilter {
	private Map<String, Map<String, Set<String>>> mappedKeySet = new ConcurrentHashMap<String, Map<String, Set<String>>>();

	@Override
	public boolean permit(Message msg) { 
		String key = msg.getKey();
		if(key == null) return true;
		
		String mq = msg.getMq();
		String keyGroup = msg.getKeyGroup(); 
		int res = addKey(mq, keyGroup, key);
		return res > 0;
	}
 
	
	@Override
	public void close() throws IOException { 
		
	}

	@Override
	public int addKey(String mq, String group, String key) {
		if(key == null){
			return 0;
		}
		
		if(mq == null){
			mq = "";
		}
		if(group == null){
			group = "";
		} 
		Map<String, Set<String>> keySets = null; 
		synchronized (mappedKeySet) {
			keySets = mappedKeySet.get(mq);
			if(keySets == null){
				keySets = new ConcurrentHashMap<String, Set<String>>();
				mappedKeySet.put(mq, keySets);
			} 
			Set<String> keys = keySets.get(group);
			if(keys == null){
				keys = Collections.synchronizedSet(new HashSet<String>());
				keySets.put(group, keys);
			}
			if(keys.contains(key)) return 0;
			keys.add(key);
			return 1;
		} 
	}

	@Override
	public int removeKey(String mq, String group, String key) {
		if(mq == null){
			mq = "";
		}
		
		Map<String, Set<String>> keySets = null; 
		synchronized (mappedKeySet) {
			keySets = mappedKeySet.get(mq);
			if(keySets == null){
				return 0;
			}
			if(group == null && key == null){
				int count = 0;
				for(Set<String> keys : keySets.values()){
					count += keys.size();
				}
				mappedKeySet.remove(mq); //delete whole mq's keys
				return count;
			}
			
			if(group == null){
				group = "";
			} 
			
			Set<String> keys = keySets.get(group); 
			if(keys == null) return 0;
			
			if(key == null){
				keys =  keySets.remove(group);
				if(keys == null) return 0;
				return keys.size();
			}
			
			return keys.remove(key)? 1 : 0;
		}  
	}  
}
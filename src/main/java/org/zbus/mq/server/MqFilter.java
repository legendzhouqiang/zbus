package org.zbus.mq.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.zbus.net.http.Message;

public interface MqFilter {
	void removeKey(String mq, String key);
	boolean permit(Message msg);
}


class MemoryMqFilter implements MqFilter {
	private Set<String> keySet = Collections.synchronizedSet(new HashSet<String>());
	
	@Override
	public boolean permit(Message msg) { 
		String key = msg.getHead("key");
		if(key != null){
			key = msg.getMq() + "#" + key;
			if(keySet.contains(key)) return false;
			keySet.add(key);
		}
		return true;
	}

	@Override
	public void removeKey(String mq, String key) { 
		key = mq + "#" + key;
		keySet.remove(key);
	} 
}
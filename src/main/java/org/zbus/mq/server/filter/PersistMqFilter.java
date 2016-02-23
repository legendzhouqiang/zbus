package org.zbus.mq.server.filter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.http.Message;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;

public class PersistMqFilter implements MqFilter {
	private Environment environment = null; 
	private DatabaseConfig dbConfig;
	private Map<String, Map<String, Database>> mappedKeySet = new ConcurrentHashMap<String, Map<String,Database>>();


	public PersistMqFilter(String storePath) {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		File file = new File(storePath);
		if (!file.exists()) {
			file.mkdirs();
		}
		environment = new Environment(file, envConfig);
		dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true); 

	}

	@Override
	public void close() throws IOException { 
		for(Map<String, Database> keySets : mappedKeySet.values()){
			for(Database database : keySets.values()){
				database.close();
			}
		}
		mappedKeySet.clear();
		if (environment != null) {
			environment.close();
		}
	}

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
		Map<String, Database> keySets = null; 
		synchronized (mappedKeySet) {
			keySets = mappedKeySet.get(mq);
			if(keySets == null){
				keySets = new ConcurrentHashMap<String, Database>();
				mappedKeySet.put(mq, keySets);
			} 
			Database keys = keySets.get(group);
			if(keys == null){
				String dbName = mq+"_"+group+"_"+key;
				keys = environment.openDatabase(null, dbName, dbConfig);
				keySets.put(group, keys);
			}
			
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes());
			DatabaseEntry theData = new DatabaseEntry();
			keys.get(null, theKey, theData, LockMode.DEFAULT);
			if (theData.getData() != null) {
				return 0;
			}
			theData = new DatabaseEntry(key.getBytes());
			keys.put(null, theKey, theData); 
			return 1;
		} 
	}

	@Override
	public int removeKey(String mq, String group, String key) {
		if(mq == null){
			mq = "";
		}
		
		Map<String, Database> keySets = null; 
		synchronized (mappedKeySet) {
			keySets = mappedKeySet.get(mq);
			if(keySets == null){
				return 0;
			}
			if(group == null && key == null){
				int count = 0;
				for(Database keys : keySets.values()){
					count += keys.count(); 
					String dbName = keys.getDatabaseName();
					keys.close();
					environment.removeDatabase(null, dbName); 
				}
				mappedKeySet.remove(mq); //delete whole mq's keys
				return count;
			}
			
			if(group == null){
				group = "";
			} 
			
			Database keys = keySets.get(group); 
			if(keys == null) return 0;
			
			if(key == null){
				keys =  keySets.remove(group); 
				if(keys == null) return 0;
				int count = (int)keys.count(); 
				
				String dbName = keys.getDatabaseName();
				keys.close();
				environment.removeDatabase(null, dbName); 
				return count;
			}
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes());
			DatabaseEntry theData = new DatabaseEntry(); 
			keys.get(null, theKey, theData, LockMode.DEFAULT);
			if (theData.getData() == null) {
				return 0;
			}
			keys.removeSequence(null, theKey);
			return 1;
		}  
	}
}
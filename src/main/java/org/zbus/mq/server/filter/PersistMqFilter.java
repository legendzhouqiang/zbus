package org.zbus.mq.server.filter;

import java.io.File;
import java.io.IOException;

import org.zbus.net.http.Message;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;

public class PersistMqFilter implements MqFilter {
	private Environment environment = null;
	private Database database = null;

	public PersistMqFilter(String storePath) {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		File file = new File(storePath);
		if (!file.exists()) {
			file.mkdirs();
		} 
		environment = new Environment(file, envConfig); 
		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true);
		database = environment.openDatabase(null, "MqFilter", dbConfig); 
	}

	@Override
	public void close() throws IOException {
		if (database != null) {
			database.close();
		}
		if (environment != null) {
			environment.close();
		}
	}

	@Override
	public boolean permit(Message msg) {
		String key = msg.getHead("key");
		if (key != null) {
			try {
				key = msg.getMq() + "#" + key;
				DatabaseEntry theKey = new DatabaseEntry(key.getBytes());
				DatabaseEntry theData = new DatabaseEntry();
				database.get(null, theKey, theData, LockMode.DEFAULT);
				if (theData.getData() != null) {
					return false;
				} 
				theData = new DatabaseEntry(key.getBytes());
				database.put(null, theKey, theData); 
			} catch (Exception ex) {
				System.err.println(ex);
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeKey(String mq, String key) {
		key = mq + "#" + key;
		try { 
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes()); 
			database.delete(null, theKey);  
		} catch (Exception ex) {
			//ignore
		}
	} 
}
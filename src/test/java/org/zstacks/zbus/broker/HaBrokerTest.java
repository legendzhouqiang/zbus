package org.zstacks.zbus.broker;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.HaBroker;
import org.zstacks.zbus.client.broker.HaBrokerConfig;

public class HaBrokerTest {

	public static void main(String[] args) throws Exception {  
	
		HaBrokerConfig config = new HaBrokerConfig(); 
		config.setTrackAddrList("127.0.0.1:16666");
		Broker broker = new HaBroker(config);
		
		broker.close();
	}
}

package org.zbus.examples;

import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;
import org.zbus.net.EventDriver;

public class ZbusStarter {

	@SuppressWarnings("resource")
	public static void main_simple(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555; 
		config.verbose = true; //print out message
		config.storePath = "/tmp/zbus/mq";   
		
		EventDriver eventDriver = new EventDriver();
		//eventDriver.setSslContextOfSelfSigned(); //Enable SSL =on zbus
		
		config.setEventDriver(eventDriver); 
		final MqServer server = new MqServer(config);  
		server.start();  
	}
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.loadFromXml("conf/zbus.xml");
		
		EventDriver eventDriver = new EventDriver();
		//eventDriver.setSslContextOfSelfSigned(); //Enable SSL on zbus
		
		config.setEventDriver(eventDriver); 
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

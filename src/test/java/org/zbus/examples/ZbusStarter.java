package org.zbus.examples;

import java.io.InputStream;

import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;
import org.zbus.net.IoDriver;

public class ZbusStarter {

	@SuppressWarnings("resource")
	public static void main_simple(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555; 
		config.verbose = true; //print out message 
		
		IoDriver eventDriver = new IoDriver();
		//eventDriver.setSslContextOfSelfSigned(); //Enable SSL =on zbus
		
		config.setEventDriver(eventDriver); 
		final MqServer server = new MqServer(config);  
		server.start();  
	}
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig(); 
		InputStream stream = ZbusStarter.class.getClassLoader()
                .getResourceAsStream("conf/zbus.xml");
		config.loadFromXml(stream);
		
		IoDriver eventDriver = new IoDriver();
		//eventDriver.setSslContextOfSelfSigned(); //Enable SSL on zbus
		
		config.setEventDriver(eventDriver); 
		final MqServer server = new MqServer(config);  
		server.start();  
	}

}

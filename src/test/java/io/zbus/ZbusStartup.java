package io.zbus;

import java.io.InputStream;

import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;

public class ZbusStartup {  
	
	@SuppressWarnings("resource")
	public static void main2(String[] args) throws Exception { 
		MqServerConfig config = new MqServerConfig();   
		config.serverPort = 15555;   
		
		final MqServer server = new MqServer(config);  
		server.start();  
	}  
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		InputStream stream = ZbusStartup.class.getClassLoader().getResourceAsStream("conf/zbus.xml");
		
		MqServerConfig config = new MqServerConfig(); 
		config.loadFromXml(stream);  
		  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	} 
}

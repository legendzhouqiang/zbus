package org.zbus.examples.ha;

import java.io.InputStream;

import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;

public class Zbus1 { 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		InputStream stream = Zbus1.class.getClassLoader().getResourceAsStream("conf/ha/zbus1.xml");
		
		MqServerConfig config = new MqServerConfig(); 
		config.loadFromXml(stream);  
		  
		
		final MqServer server = new MqServer(config);  
		server.start();  
	} 
}

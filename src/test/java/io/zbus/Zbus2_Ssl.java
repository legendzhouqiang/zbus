package io.zbus;

import io.zbus.mq.server.MqServer;

public class Zbus2_Ssl {   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		new MqServer("conf/zbus2_ssl.xml").start();  
	}  
}

package io.zbus;

import io.zbus.mq.server.MqServer;

public class Zbus1_Ssl {   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		new MqServer("conf/zbus1_ssl.xml").start();  
	}  
}

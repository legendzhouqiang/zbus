package io.zbus.unittests.mq.server;

import io.zbus.mq.server.MqServer;

public class MqServerTest {

	public static void main(String[] args) throws Exception {  
		MqServer server = new MqServer();
		server.start(); 
		
		Thread.sleep(1000);
		server.close();
	} 
}

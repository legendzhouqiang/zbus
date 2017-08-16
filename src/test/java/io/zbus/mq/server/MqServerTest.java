package io.zbus.mq.server;

public class MqServerTest {

	public static void main(String[] args) throws Exception {  
		MqServer server = new MqServer();
		server.start(); 
		
		Thread.sleep(1000);
		server.close();
	} 
}

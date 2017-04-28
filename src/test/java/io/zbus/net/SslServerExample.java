package io.zbus.net;

import io.zbus.mq.net.MessageAdaptor;
import io.zbus.mq.net.MessageServer;

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		EventDriver driver = new EventDriver(); 
		driver.setServerSslContext("ssl/zbus.crt", "ssl/zbus.key");
		
		MessageServer server = new MessageServer(driver); 
		
		server.start(15555, new MessageAdaptor());  
	} 
}

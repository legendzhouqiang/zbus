package org.zbus.unitests.net;

import org.zbus.net.IoDriver;
import org.zbus.net.http.MessageClient;

public class ClientTest {

	public static void main(String[] args) throws Exception { 
		IoDriver driver = new IoDriver();
		
		MessageClient client = new MessageClient("127.0.0.1:8080", driver);
		client.ensureConnectedAsync();
		Thread.sleep(100);
		client.close();
		driver.close();
	}

}

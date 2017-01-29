package io.zbus.net;

import io.zbus.mq.net.MessageClient;

public class NetTest {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		EventDriver driver = new EventDriver();
		MessageClient client = new MessageClient("127.0.0.1:15555", driver);
		client.ensureConnectedAsync();
	}
}

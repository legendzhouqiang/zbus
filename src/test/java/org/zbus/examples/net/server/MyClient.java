package org.zbus.examples.net.server;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MyClient {

	public static void main(String[] args) throws Exception {
		final SelectorGroup selectorGroup = new SelectorGroup();

		final MessageClient client = new MessageClient("127.0.0.1:8080", selectorGroup);

		Message msg = new Message();
		msg.setCmd("/hello"); 

		msg.setBody("hello world");
		msg = client.invokeSync(msg, 5000);
		System.out.println(msg);
		
		client.close();
		
		selectorGroup.close();
	}
}

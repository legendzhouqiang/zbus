package org.zbus.net;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MyClient {

	public static void main(String[] args) throws Exception {  
		final Dispatcher dispatcher = new Dispatcher();  
		
		final MessageClient client = new MessageClient("127.0.0.1:80", dispatcher);
		
		Message msg = new Message();
		msg.setCmd("hello"); 
		msg.setBody("hello world"); 
		Message res = client.invokeSync(msg);
		System.out.println(res); 
		
		client.close();
		dispatcher.close();
	}
}

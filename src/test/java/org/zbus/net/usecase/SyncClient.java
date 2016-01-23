package org.zbus.net.usecase;


import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class SyncClient {

	public static void main(String[] args) throws Exception { 
		SelectorGroup dispatcher = new SelectorGroup();

		final MessageClient client = new MessageClient("127.0.0.1:80", dispatcher);
	
		Message msg = new Message();
		msg.setCmd("hello");
		msg.setBody("hello");
		Message res = client.invokeSync(msg); //同步请求
		System.out.println(res);
		
		
		client.close();
		dispatcher.close(); 
	}

}

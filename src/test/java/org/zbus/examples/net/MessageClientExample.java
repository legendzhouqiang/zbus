package org.zbus.examples.net;

import org.zbus.kit.ConfigKit;
import org.zbus.net.IoDriver;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MessageClientExample { 
 
	public static void main(String[] args) throws Exception {
		String address = ConfigKit.option(args, "-h", "127.0.0.1:8080");
		 
		IoDriver driver = new IoDriver();
		
		MessageClient client = new MessageClient(address, driver); 
		
		try { 
			Message msg = new Message();  
			msg.setBody("hello");
			Message res = client.invokeSync(msg);
			System.out.println(res);
			
			
		} finally {
			client.close();  
			driver.close(); 
		}

		System.out.println("---done---");
	}
}

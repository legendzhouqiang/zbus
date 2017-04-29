package io.zbus.net;

import io.zbus.mq.Message;
import io.zbus.mq.net.MessageClient;

public class SslClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventDriver driver = new EventDriver(); 
		driver.setClientSslContext("ssl/zbus.crt");   
		
		MessageClient client = new MessageClient("localhost:15555", driver);

		Message req = new Message();
		Message res = client.invokeSync(req);
		
		System.out.println(res);
		
		
		client.close();
		driver.close();
	}

}

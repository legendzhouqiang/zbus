package org.zbus.agent;

import org.zbus.client.ClientBuilder;
import org.zbus.client.Consumer;
import org.zbus.client.builder.SimpleClientBuilder;
import org.zbus.remoting.Message;



public class ConsumerWithFactory {

	public static void main(String[] args) throws Exception{  
		ClientBuilder factory = new SimpleClientBuilder("127.0.0.1:15555");
		
		Consumer consumer = new Consumer(factory, "MyMQ"); 
		int i=0; 
		while(true){
			Message msg = consumer.recv(10000);
			if(msg == null) continue;
			i++;
			if(i%1==0)
			System.out.format("================%04d===================\n%s\n", 
					i, msg); 
		} 
		
		//consumer.close();
	}

}

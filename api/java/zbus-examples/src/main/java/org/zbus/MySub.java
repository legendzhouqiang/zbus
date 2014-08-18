package org.zbus;

import org.zbus.client.Consumer;
import org.zbus.common.MessageMode;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;

public class MySub {

	public static void main(String[] args) throws Exception {  
		
		final RemotingClient client = new RemotingClient("127.0.0.1:15555");	
		final Consumer consumer = new Consumer(client, "MySub", MessageMode.PubSub);   
		consumer.setTopic("qhee,xmee");  
		while(true){
			Message msg = consumer.recv(10000); 
			if(msg == null) continue;
			System.out.println(msg);
		}
	}

}


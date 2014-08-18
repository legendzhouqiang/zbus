package org.zbus.agent;

import org.zbus.client.Consumer;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.ha.ClientAgent;
import org.zbus.remoting.Message;



public class ConsumerWithAgent {

	public static void main(String[] args) throws Exception{  
		AgentConfig config = new AgentConfig();
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667"); 
		
		ClientAgent agent = new ClientAgent(config);
		  
		Consumer consumer = new Consumer(agent, "MyMQ"); 
		int i=0; 
		while(true){
			Message msg = consumer.recv(10000);
			if(msg == null) continue;
		
			i++;
			if(i%1==0)
			System.out.format("================%04d===================\n%s\n", 
					i, msg); 
		} 
		
	}

}

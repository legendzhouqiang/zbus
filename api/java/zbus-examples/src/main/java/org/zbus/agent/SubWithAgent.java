package org.zbus.agent;

import org.zbus.client.Consumer;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.ha.ClientAgent;
import org.zbus.common.MessageMode;
import org.zbus.remoting.Message;

public class SubWithAgent {

	public static void main(String[] args) throws Exception {  
		AgentConfig config = new AgentConfig();
		config.setSeedBroker("127.0.0.1:15555");
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		
		ClientAgent agent = new ClientAgent(config); 
		final Consumer sub = new Consumer(agent, "MySub", MessageMode.PubSub);   
 
		sub.setTopic("qhee,xmee");
		int i=1;
		while(true){
			Message msg = sub.recv(10000);
			if(msg == null) continue;
			System.out.println(i++ + ":"+msg);
		}
		
		//agent.destroy();
	}

}

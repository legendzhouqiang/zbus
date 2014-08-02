package org.zbus.agent.rpc;

import org.remoting.Message;
import org.zbus.client.ha.ClientAgent;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.rpc.Rpc;


public class RpcWithAgent {
	
	public static void main(String[] args) throws Throwable {
		//1 创建 TackAgent
		AgentConfig config = new AgentConfig();
		config.setTrackServerList("127.0.0.1:16666");
		ClientAgent agent = new ClientAgent(config);
		 
		Rpc rpc = new Rpc(agent, "MyRpc");  
		
		for(int i=0;i<100;i++){
			Message req = new Message(); 
			req.setBody("hello from client"); 
			
			Message reply = rpc.invokeSync(req, 10000); 
			System.out.println(reply);
		}
		
		System.out.println("--done--");
		
	}
}

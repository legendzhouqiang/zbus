package org.zbus.agent.rpc;

import org.zbus.client.agent.AgentConfig;
import org.zbus.client.agent.ClientAgent;
import org.zbus.client.rpc.json.JsonRpcProxy;
import org.zbus.rpc.json.ServiceInterface;


public class JsonRpcWithAgent {
	
	public static void main(String[] args) throws Throwable {
		//1 创建 TackAgent
		AgentConfig config = new AgentConfig();
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		ClientAgent agent = new ClientAgent(config);
		
		//2)通过RpcProxy动态创建实现类
		ServiceInterface rpc = new JsonRpcProxy(agent).getService(
				ServiceInterface.class, "mq=MyJsonRpc"); 
	
		for(int i=0;i<100;i++){
			String pong = rpc.echo("hong"); 
			System.out.println(i + ":" + pong);  
		}
		
	}
}

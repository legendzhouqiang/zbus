package org.zbus.agent.rpc;

import org.zbus.client.ha.ClientAgent;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.json.JsonServiceHandler;
import org.zbus.rpc.json.ServiceImpl;
 
 
public class JsonServiceWithAgent {
	public static void main(String[] args) throws Exception {
		//1) 创建TrackAgent实例
		AgentConfig config = new AgentConfig();
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		ClientAgent agent = new ClientAgent(config);
		
		JsonServiceHandler handler = new JsonServiceHandler(); 
		handler.addModule("ServiceInterface", new ServiceImpl());  

		RpcServiceConfig rpcConfig = new RpcServiceConfig();
		rpcConfig.setClientBuilder(agent); //采用TrackAgent
		rpcConfig.setServiceName("MyJsonRpc");
		rpcConfig.setThreadCount(4);
		rpcConfig.setServiceHandler(handler); 
		
		RpcService svc = new RpcService(rpcConfig);
		svc.start(); 
	}
}

package org.zbus.agent.rpc;

import org.remoting.Helper;
import org.remoting.Message;
import org.zbus.client.ha.ClientAgent;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.ServiceHandler;

public class ServiceWithAgent {
	
	public static void main(String[] args) throws Exception {  
		String trackServerList = Helper.option(args, "-track", "127.0.0.1:16666"); 
		int threadCount = Helper.option(args, "-c", 4);
		String service = Helper.option(args, "-s", "MyRpc");
		
		AgentConfig config = new AgentConfig();
		config.setTrackServerList(trackServerList);
		ClientAgent agent = new ClientAgent(config);
		
		RpcServiceConfig rpcConfig = new RpcServiceConfig();
		rpcConfig.setThreadCount(threadCount); 
		rpcConfig.setServiceName(service); 
		rpcConfig.setClientBuilder(agent);
		
		rpcConfig.setServiceHandler(new ServiceHandler() { 
			@Override
			public Message handleRequest(Message request) { 
				request.setStatus("200");
				request.setBody("Server time: "+System.currentTimeMillis());
				
				return request;
			}
		});
	
		
		RpcService svc = new RpcService(rpcConfig);
		svc.start();  
	} 
}

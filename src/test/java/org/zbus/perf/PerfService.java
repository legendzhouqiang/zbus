package org.zbus.perf;

import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.ServiceHandler;
import org.zbus.common.Helper;
import org.zbus.remoting.Message;

public class PerfService {
	
	public static void main(String[] args) throws Exception {  
		String broker = Helper.option(args, "-b", "127.0.0.1:15555"); 
		int threadCount = Helper.option(args, "-c", 8);
		String service = Helper.option(args, "-s", "MqService");
		
		RpcServiceConfig config = new RpcServiceConfig();
		config.setThreadCount(threadCount); 
		config.setServiceName(service);
		config.setBroker(broker);
		
		config.setServiceHandler(new ServiceHandler() { 
			@Override
			public Message handleRequest(Message request) { 
				request.setStatus("200");
				request.setBody("Server time: "+System.currentTimeMillis());
				
				return request;
			}
		});
	
		
		RpcService svc = new RpcService(config);
		svc.start(); 
		
		
		System.out.println("running...");
	} 
}

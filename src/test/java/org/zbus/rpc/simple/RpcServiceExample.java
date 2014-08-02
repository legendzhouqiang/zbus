package org.zbus.rpc.simple;

import org.remoting.Helper;
import org.remoting.Message;
import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.ServiceHandler;

public class RpcServiceExample {
	
	public static void main(String[] args) throws Exception {  
		String broker = Helper.option(args, "-b", "127.0.0.1:15555"); 
		
		int threadCount = Helper.option(args, "-c", 1);
		String service = Helper.option(args, "-s", "MyRpc");
		
		RpcServiceConfig config = new RpcServiceConfig();
		config.setThreadCount(threadCount); 
		config.setServiceName(service);
		config.setBroker(broker); 
		
		config.setServiceHandler(new ServiceHandler() { 
			@Override
			public Message handleRequest(Message request) { 
				System.out.println(request);
				Message result = new Message();
				result.setStatus("200");
				result.setBody("Server time: "+System.currentTimeMillis());
				
				return result;
			}
		});
	
		
		RpcService svc = new RpcService(config);
		svc.start();  
	} 
}

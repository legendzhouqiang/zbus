package org.zbus.rpc.json;

import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.json.JsonServiceHandler;
 
 
public class JsonServiceWithClient {
	public static void main(String[] args) throws Exception {
		
		//采用JSON格式处理
		JsonServiceHandler handler = new JsonServiceHandler(); 
		//增加模块，模块名在调用时需要指定
		handler.addModule(ServiceInterface.class, new ServiceImpl());  
		//handler.addModule("ServiceInterface", new ServiceImpl()); //可以指定模块名
		
		//下面是通用的启动RPC配置
		RpcServiceConfig rpcConfig = new RpcServiceConfig();
		rpcConfig.setBroker("127.0.0.1:15555");
		rpcConfig.setServiceName("MyJsonRpc");
		rpcConfig.setThreadCount(4);
		rpcConfig.setServiceHandler(handler); 
		
		RpcService svc = new RpcService(rpcConfig);
		svc.start(); 
	}
}

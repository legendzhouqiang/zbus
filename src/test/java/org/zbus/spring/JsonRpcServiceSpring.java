package org.zbus.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zbus.client.rpc.RpcService;
import org.zbus.client.rpc.RpcServiceConfig;
import org.zbus.client.rpc.json.JsonServiceHandler;
 
public class JsonRpcServiceSpring {
	public static void main(String[] args) throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("ZbusSpring.xml");
		
		//业务配置，增加模块
		ServiceInterface service = (ServiceInterface) context.getBean("serviceInterface");
		JsonServiceHandler handler = new JsonServiceHandler(); 
		handler.addModule(ServiceInterface.class, service);
		
		
		
		//获取配置
		RpcServiceConfig config = (RpcServiceConfig)context.getBean("jsonrpcConfig");
		config.setServiceHandler(handler);
		
		RpcService rpcService = new RpcService(config);
		rpcService.start();
	}
}

package org.zbus;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.rpc.RpcServiceHandler;
import org.zbus.client.service.ServiceHandler;
import org.zbus.client.service.ServiceLoader;
import org.zbus.client.service.ServiceProvider;

class UserModule{
	public String getName(){
		return "hong";
	}
}

public class RpcServiceProvider implements ServiceProvider {
	
	@Override
	public ServiceHandler buildHandler() { 
		RpcServiceHandler handler = new RpcServiceHandler(); 
		//增加模块，模块名在调用时需要指定
		handler.addModule(new UserModule());  
		return handler;
	} 

	
	public static void main(String[] args) throws Exception {
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		
		@SuppressWarnings("resource")
		ServiceLoader serviceLoader = new ServiceLoader(broker);
		serviceLoader.loadFromServiceBase("D:\\zbus-project\\zbus\\zbus-dist\\work"); 
	}
}

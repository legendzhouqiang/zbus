package org.zbus.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcInvoker;

public class DirectRpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(brokerConfig); 
		
		
		RpcInvoker rpc = new RpcInvoker(broker);    
		 
		String res = rpc.invokeSync(String.class, "echo","test");
		//调用重载的方法需要指定类型参数
		//res = rpc.invokeSync(String.class, "getString", new Class[]{String.class, int.class}, "version2", 2); 
		System.out.println(res);
		
		
		broker.close();
	}
}

package org.zbus.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcInvoker;

public class MqRpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);  
		
		
		MqInvoker messageInvoker = new MqInvoker(broker, "MyRpc"); 
		RpcInvoker rpc = new RpcInvoker(messageInvoker);     
		rpc.setVerbose(true);
		
		Integer res = rpc.invokeSync(Integer.class, "plus", 1,2); 
		System.out.println(res); 
		
		String res2 = rpc.invokeSync(String.class, "getString",
				new Class[]{String.class, int.class}, "string", 2); 
		System.out.println(res2); 
			
		
		
		broker.close();
	}
}

package org.zbus.examples.gateway;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcInvoker; 

public class DirectCallTargetClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(brokerConfig);  
		
		RpcInvoker rpc = new RpcInvoker(broker);    
		 
		Object res = rpc.invokeSync(String.class, "echo", "test");
		System.out.println(res); 
		
		broker.close();
	}
}

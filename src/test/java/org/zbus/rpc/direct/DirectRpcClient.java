package org.zbus.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcInvoker;

public class DirectRpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig); 
		
		
		RpcInvoker rpc = new RpcInvoker(broker);    
		
		for(int i=0;i<1000000;i++){
			String res = rpc.invokeSync(String.class, "getString", "testxxxx"); 
			System.out.println(res);
		}
		
		broker.close();
	}
}

package org.zbus.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.rpc.RpcInvoker;

public class HaRpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		Broker broker = new HaBroker(brokerConfig);
	  
		HaInvoker messageInvoker = new HaInvoker(broker, "MyRpc");
		
		RpcInvoker rpc = new RpcInvoker(messageInvoker);    
		
		for(int i=0;i<100000;i++){
			try{
				String res = rpc.invokeSync(String.class, "getString", "testxxxx"); 
				System.out.println(res);
			}catch(Exception e){
				//
			}
		}
		
		broker.close();
	}
}

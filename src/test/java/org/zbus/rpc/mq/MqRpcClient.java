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
		
		for(int i=0;i<10;i++){
			try{
				String res = rpc.invokeSync(String.class, "echo", "testxxxx"); 
				System.out.println(res);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		broker.close();
	}
}

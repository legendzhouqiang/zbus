package org.zbus.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcInvoker;

public class RpcRawExample {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		Broker broker = new HaBroker(brokerConfig);
	  
		MessageInvoker invoker = new HaInvoker(broker, "MyRpc");
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString", "testxxxx");
		
		System.out.println(res);
		
		broker.close();
	}
}

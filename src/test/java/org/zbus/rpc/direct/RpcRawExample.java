package org.zbus.rpc.direct;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.direct.DirectInvoker;

public class RpcRawExample {

	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(config);
	
		MessageInvoker invoker = new DirectInvoker(broker);
		
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString", "test");
		
		System.out.println(res);
		
		broker.close();
	}
}

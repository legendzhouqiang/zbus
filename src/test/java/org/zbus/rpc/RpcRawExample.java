package org.zbus.rpc;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.hub.MqInvoker;

public class RpcRawExample {

	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
	
		//MessageInvoker invoker = new DirectInvoker(broker);
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc2");
		
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString2", "testxxxx");
		
		System.out.println(res);
		
		broker.close();
	}
}

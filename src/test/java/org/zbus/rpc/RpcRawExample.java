package org.zbus.rpc;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.broking.BrokingInvoker;

public class RpcRawExample {

	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
	
		//MessageInvoker invoker = new DirectInvoker(broker);
		MessageInvoker invoker = new BrokingInvoker(broker, "MyMQ");
		
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString", "testxxxx");
		
		System.out.println(res);
		
		broker.close();
	}
}

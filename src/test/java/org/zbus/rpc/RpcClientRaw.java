package org.zbus.rpc;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClientRaw {

	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
	 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");
		
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString", "testxxxx");
		
		System.out.println(res);
		
		broker.close();
	}
}

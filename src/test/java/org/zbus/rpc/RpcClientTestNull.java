package org.zbus.rpc;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.biz.Interface;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClientTestNull {

	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");
		RpcInvoker rpc = new RpcInvoker(invoker); 
		RpcFactory factory = new RpcFactory(invoker);

		// 3) 动态代理出实现类
		Interface hello = factory.getService(Interface.class);
 
		String res = hello.getString(null);
		System.out.println(res);
		
		
		Object obj = rpc.invokeSync("getString", (Object)null);
		System.out.println(obj);
		
		broker.close();
	}
}

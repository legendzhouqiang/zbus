package org.zbus.examples.gateway;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.mq.MqInvoker;

public class GatewayClient {

	public static void main(String[] args) throws Exception {
		// 1)创建Broker代表（可用高可用替代）
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(config);

		// 2)创建基于MQ的Invoker以及Rpc工厂，指定RPC采用的MQ为MyRpc
		MessageInvoker invoker = new MqInvoker(broker, "MyGateway");
		RpcFactory factory = new RpcFactory(invoker);

		// 3) 动态代理出实现类
		InterfaceExample hello = factory.getService(InterfaceExample.class);
 
		Object res = hello.echo("test");
		System.out.println(res);
		
		broker.close(); 
	}

}

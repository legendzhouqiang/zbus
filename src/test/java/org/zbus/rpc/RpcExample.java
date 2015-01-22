package org.zbus.rpc;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.rpc.RpcProxy;
import org.zbus.rpc.biz.ServiceInterface;

public class RpcExample {
	public static void main(String[] args) throws Exception {
		// 1）创建Broker代表 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker);
		
		ServiceInterface rpc = proxy.getService(
				ServiceInterface.class,
				"mq=MyRpc&&timeout=1000");

		try {
			int c = rpc.plus(1, 2);
			System.out.println(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < 100; i++) {
			String echo = rpc.echo("hong"+i);
			System.out.println(echo);

		}
	}
}

package org.zbus.ha;

import org.zbus.client.Broker;
import org.zbus.client.broker.HaBroker;
import org.zbus.client.broker.HaBrokerConfig;
import org.zbus.client.rpc.RpcProxy;
import org.zbus.rpc.biz.ServiceInterface;

public class RpcExample {
	public static void main(String[] args) throws Exception {
		// 1）创建Broker代表 
		HaBrokerConfig brokerConfig = new HaBrokerConfig();
		brokerConfig.setTrackAddrList("127.0.0.1:16666:127.0.0.1:16667");
		Broker broker = new HaBroker(brokerConfig);

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

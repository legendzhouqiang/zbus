package org.zstacks.zbus.rpc;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;
import org.zstacks.zbus.rpc.biz.Interface;

public class RpcTimeoutExample {


	public static void main(String[] args) throws Exception { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc&&timeout=1000");
		
		hello.testTimeout();
		
		broker.close();
	}
}

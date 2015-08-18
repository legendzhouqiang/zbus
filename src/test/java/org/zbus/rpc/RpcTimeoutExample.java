package org.zbus.rpc;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.rpc.RpcProxy;
import org.zbus.rpc.biz.Interface;

public class RpcTimeoutExample {


	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc&&timeout=1000");
		
		hello.testTimeout();
		
		broker.close();
	}
}

package org.zbus.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.RpcConfig;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.biz.Interface;
import org.zbus.rpc.hub.HubInvoker;

public class RpcTimeoutExample {


	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config); 
		MessageInvoker invoker = new HubInvoker(broker, "MyRpc");
		
		RpcFactory proxy = new RpcFactory(invoker); 
		
		RpcConfig rpcConfig = new RpcConfig();
		rpcConfig.setTimeout(1000);
		Interface hello = proxy.getService(Interface.class, rpcConfig);
		
		hello.testTimeout();
		
		broker.close();
	}
}

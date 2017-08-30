package org.zbus.ha.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.examples.rpc.RpcCases;
import org.zbus.examples.rpc.appdomain.InterfaceExample;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.direct.HaInvoker;

public class RpcClient {

	public static void main(String[] args) throws Exception { 
		Broker broker = new ZbusBroker("[127.0.0.1:16666]"); 
		
		//HA 模式invoke
		HaInvoker invoker = new HaInvoker(broker, "HaDirectRpc");
		
		RpcFactory factory = new RpcFactory(invoker); //directly using broker as invoker 
		InterfaceExample hello = factory.getService(InterfaceExample.class);
		
		RpcCases.testDynamicProxy(hello); //test cases
		
		broker.close();
	}
}

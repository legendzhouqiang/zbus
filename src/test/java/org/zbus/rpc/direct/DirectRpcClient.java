package org.zbus.rpc.direct;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.rpc.RpcCodec.Response;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.biz.Order;

public class DirectRpcClient {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig(); 
		brokerConfig.setServerAddress("127.0.0.1:8080");
		Broker broker = new SingleBroker(brokerConfig); 
		
		
		RpcInvoker rpc = new RpcInvoker(broker);    
		 
		Object res = rpc.invokeSync(String.class, "echo", "test");
		System.out.println(res);
		//调用重载的方法需要指定类型参数
		res = rpc.invokeSync(String.class, "getString", new Class[]{String.class, int.class}, "version2", 2); 
		System.out.println(res);
		
		res = rpc.invokeSync("getOrder"); 
		System.out.println(res);
		
		Request req;
		req = new Request().method("getOrder");
		Response resp = rpc.invokeSync(req);
		System.out.println(resp.getResult());
		
		Order order = rpc.invokeSync(Order.class, req);
		System.out.println(order);
		
		
		broker.close();
	}
}
